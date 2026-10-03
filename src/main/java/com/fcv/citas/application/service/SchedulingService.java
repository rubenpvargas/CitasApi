package com.fcv.citas.application.service;

import com.fcv.citas.application.exception.BusinessException;
import com.fcv.citas.application.exception.BusinessRuleException;
import com.fcv.citas.application.exception.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchedulingService {
    private final JdbcTemplate jdbc;
    private final java.time.Clock clock;

    public SchedulingService(JdbcTemplate jdbc, java.time.Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public Map<String, Object> book(long userId, Long specialtyId, long professionalId, String locationCode, LocalDateTime startAt, String reason, boolean general) {
        Map<String, Object> specialty = general
                ? one("SELECT id,appointment_duration_minutes durationMinutes FROM specialties WHERE is_general=TRUE AND active=TRUE")
                : one("SELECT id,appointment_duration_minutes durationMinutes FROM specialties WHERE id=? AND is_general=FALSE AND active=TRUE", specialtyId);
        long sid = ((Number) specialty.get("id")).longValue();
        int duration = ((Number) specialty.get("durationMinutes")).intValue();
        List<Boolean> professionalActive = jdbc.queryForList("SELECT active FROM professionals WHERE id=?", Boolean.class, professionalId);
        if (professionalActive.isEmpty()) throw new NotFoundException("Professional not found");
        // HU-011 CA-03: un profesional inactivo no puede reservarse.
        if (!professionalActive.getFirst()) throw new BusinessRuleException("PROFESSIONAL_INACTIVE", "The professional is not active");
        long locationId = locationId(locationCode);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM professional_specialties WHERE professional_id=? AND specialty_id=? AND active=TRUE", Integer.class, professionalId, sid) == 0)
            throw new BusinessException("SPECIALTY_NOT_ASSIGNED", "The professional does not provide this specialty");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM professional_locations WHERE professional_id=? AND location_id=? AND active=TRUE", Integer.class, professionalId, locationId) == 0)
            throw new BusinessException("LOCATION_NOT_ASSIGNED", "The professional is not enabled at this location");
        LocalDateTime endAt = startAt.plusMinutes(duration);
        List<Map<String,Object>> slots = jdbc.queryForList("SELECT ps.id,ps.start_at startAt FROM professional_slots ps JOIN availability_blocks b ON b.id=ps.availability_block_id WHERE b.professional_id=? AND b.location_id=? AND b.active=TRUE AND ps.start_at>=? AND ps.end_at<=? AND ps.appointment_id IS NULL AND ps.reschedule_request_id IS NULL ORDER BY ps.start_at FOR UPDATE", professionalId, locationId, (startAt), (endAt));
        ensureConsecutive(slots, startAt, duration);
        String status = general ? "APPROVED" : "REQUESTED";
        long statusId = statusId("appointment_statuses", status);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> { PreparedStatement ps=connection.prepareStatement("INSERT INTO appointments(patient_user_id,professional_id,location_id,specialty_id,status_id,reason,scheduled_start_at,scheduled_end_at,created_by_user_id,approved_at,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,NOW(6),NOW(6))", Statement.RETURN_GENERATED_KEYS); ps.setLong(1,userId);ps.setLong(2,professionalId);ps.setLong(3,locationId);ps.setLong(4,sid);ps.setLong(5,statusId);ps.setString(6,reason);ps.setObject(7,startAt);ps.setObject(8,endAt);ps.setLong(9,userId); if(general) ps.setObject(10,LocalDateTime.now(clock)); else ps.setNull(10,java.sql.Types.TIMESTAMP); return ps; }, keys);
        long appointmentId=keys.getKey().longValue();
        jdbc.update("UPDATE professional_slots ps JOIN availability_blocks b ON b.id=ps.availability_block_id SET ps.appointment_id=? WHERE b.professional_id=? AND b.location_id=? AND ps.start_at>=? AND ps.end_at<=?", appointmentId,professionalId,locationId,(startAt),(endAt));
        jdbc.update("INSERT INTO appointment_status_history(appointment_id,status_id,changed_by_user_id,change_source,reason,changed_at) VALUES(?,?,?,'SYSTEM',?,NOW(6))", appointmentId,statusId,userId,general?"Automatic approval for general medicine":"Specialized request created");
        return one("SELECT a.id,a.scheduled_start_at startAt,a.scheduled_end_at endAt,st.code status,s.name specialtyName,l.code locationCode,p.id professionalId,u.first_name professionalFirstName,u.last_name professionalLastName FROM appointments a JOIN appointment_statuses st ON st.id=a.status_id JOIN specialties s ON s.id=a.specialty_id JOIN locations l ON l.id=a.location_id JOIN professionals p ON p.id=a.professional_id JOIN users u ON u.id=p.user_id WHERE a.id=?", appointmentId);
    }

    public List<Map<String, Object>> appointments(long userId, String status, LocalDate from, LocalDate to) {
        StringBuilder sql=new StringBuilder("SELECT a.id,a.scheduled_start_at startAt,a.scheduled_end_at endAt,st.code status,a.reason,s.code specialtyCode,s.name specialtyName,s.appointment_duration_minutes durationMinutes,l.code locationCode,l.name locationName,p.id professionalId,u.first_name professionalFirstName,u.last_name professionalLastName FROM appointments a JOIN appointment_statuses st ON st.id=a.status_id JOIN specialties s ON s.id=a.specialty_id JOIN locations l ON l.id=a.location_id JOIN professionals p ON p.id=a.professional_id JOIN users u ON u.id=p.user_id WHERE a.patient_user_id=?"); List<Object> args=new ArrayList<>(List.of(userId));
        if(status!=null){sql.append(" AND st.code=?");args.add(status);} if(from!=null){sql.append(" AND DATE(a.scheduled_start_at)>=?");args.add(from);} if(to!=null){sql.append(" AND DATE(a.scheduled_start_at)<=?");args.add(to);} sql.append(" ORDER BY a.scheduled_start_at"); return jdbc.queryForList(sql.toString(),args.toArray());
    }

    @Transactional
    public void cancel(long userId,long appointmentId){ Map<String,Object>a=ownedAppointment(userId,appointmentId); String status=(String)a.get("status"); if(!"APPROVED".equals(status)&&!"REQUESTED".equals(status))throw new BusinessException("INVALID_TRANSITION","This appointment cannot be cancelled"); if(!asDateTime(a.get("startAt")).isAfter(LocalDateTime.now(clock)))throw new BusinessException("PAST_APPOINTMENT","Only future appointments can be cancelled"); transitionAppointment(appointmentId,"CANCELLED",userId,"USER","Cancelled by patient"); releaseAppointmentSlots(appointmentId); }

    @Transactional
    public void decideAppointment(long adminId,long appointmentId,boolean approve,String reason){ Map<String,Object>a=one("SELECT a.id,st.code status FROM appointments a JOIN appointment_statuses st ON st.id=a.status_id WHERE a.id=?",appointmentId); if(!"REQUESTED".equals(a.get("status")))throw new BusinessException("INVALID_TRANSITION","Only requested appointments can be decided"); if(!approve&& (reason==null||reason.isBlank()))throw new BusinessException("REJECTION_REASON_REQUIRED","A rejection requires a reason"); if(approve) {jdbc.update("UPDATE appointments SET status_id=?,approved_by_user_id=?,approved_at=NOW(6),updated_at=NOW(6) WHERE id=?",statusId("appointment_statuses","APPROVED"),adminId,appointmentId); jdbc.update("INSERT INTO appointment_status_history(appointment_id,status_id,changed_by_user_id,change_source,reason,changed_at) VALUES(?,?,?,'ADMIN',?,NOW(6))",appointmentId,statusId("appointment_statuses","APPROVED"),adminId,reason);} else {transitionAppointment(appointmentId,"REJECTED",adminId,"ADMIN",reason);releaseAppointmentSlots(appointmentId);} }

    public List<Map<String,Object>> professionalAgenda(long userId,LocalDate from,LocalDate to,String locationCode){long p=professionalId(userId);String extra=locationCode==null?"":" AND l.code=?";return locationCode==null?jdbc.queryForList("SELECT a.id,a.scheduled_start_at startAt,a.scheduled_end_at endAt,st.code status,s.name specialtyName,l.code locationCode,u.first_name patientFirstName,u.last_name patientLastName FROM appointments a JOIN appointment_statuses st ON st.id=a.status_id JOIN specialties s ON s.id=a.specialty_id JOIN locations l ON l.id=a.location_id JOIN users u ON u.id=a.patient_user_id WHERE a.professional_id=? AND st.code='APPROVED' AND DATE(a.scheduled_start_at) BETWEEN ? AND ? ORDER BY a.scheduled_start_at",p,from,to):jdbc.queryForList("SELECT a.id,a.scheduled_start_at startAt,a.scheduled_end_at endAt,st.code status,s.name specialtyName,l.code locationCode,u.first_name patientFirstName,u.last_name patientLastName FROM appointments a JOIN appointment_statuses st ON st.id=a.status_id JOIN specialties s ON s.id=a.specialty_id JOIN locations l ON l.id=a.location_id JOIN users u ON u.id=a.patient_user_id WHERE a.professional_id=? AND st.code='APPROVED' AND DATE(a.scheduled_start_at) BETWEEN ? AND ?"+extra+" ORDER BY a.scheduled_start_at",p,from,to,locationCode);}

    @Transactional
    public void closeAppointment(long userId,long appointmentId,String outcome){long p=professionalId(userId);Map<String,Object>a=one("SELECT a.id,a.scheduled_start_at startAt,st.code status FROM appointments a JOIN appointment_statuses st ON st.id=a.status_id WHERE a.id=? AND a.professional_id=?",appointmentId,p);if(!"APPROVED".equals(a.get("status")))throw new BusinessException("INVALID_TRANSITION","Only approved appointments can be closed");if(asDateTime(a.get("startAt")).isAfter(LocalDateTime.now(clock)))throw new BusinessException("APPOINTMENT_NOT_APPLICABLE","The appointment is not applicable yet");if(!List.of("COMPLETED","NO_SHOW").contains(outcome))throw new BusinessException("INVALID_OUTCOME","Outcome must be COMPLETED or NO_SHOW");transitionAppointment(appointmentId,outcome,userId,"USER","Professional closed appointment");}

    @Transactional
    public Map<String,Object> requestReschedule(long userId,long appointmentId,LocalDateTime requestedStart,String locationCode){Map<String,Object>a=ownedAppointment(userId,appointmentId);if(!"APPROVED".equals(a.get("status")))throw new BusinessException("INVALID_TRANSITION","Only approved future appointments can be rescheduled");if(!asDateTime(a.get("startAt")).isAfter(LocalDateTime.now(clock)))throw new BusinessException("PAST_APPOINTMENT","Only future appointments can be rescheduled");if(jdbc.queryForObject("SELECT COUNT(*) FROM reschedule_requests WHERE appointment_id=? AND status_id=?",Integer.class,appointmentId,statusId("reschedule_request_statuses","PENDING"))>0)throw new BusinessException("RESCHEDULE_ALREADY_PENDING","A request is already pending");long locationId=locationId(locationCode);int duration=((Number)a.get("durationMinutes")).intValue();List<Map<String,Object>>slots=jdbc.queryForList("SELECT ps.id,ps.start_at startAt FROM professional_slots ps JOIN availability_blocks b ON b.id=ps.availability_block_id WHERE b.professional_id=? AND b.location_id=? AND ps.start_at>=? AND ps.end_at<=? AND ps.appointment_id IS NULL AND ps.reschedule_request_id IS NULL ORDER BY ps.start_at FOR UPDATE",a.get("professionalId"),locationId,(requestedStart),(requestedStart.plusMinutes(duration)));ensureConsecutive(slots,requestedStart,duration);KeyHolder k=new GeneratedKeyHolder();jdbc.update(c->{PreparedStatement ps=c.prepareStatement("INSERT INTO reschedule_requests(appointment_id,requested_by_user_id,requested_location_id,status_id,previous_start_at,previous_end_at,requested_start_at,requested_end_at,created_at) VALUES(?,?,?,?,?,?,?,?,NOW(6))",Statement.RETURN_GENERATED_KEYS);ps.setLong(1,appointmentId);ps.setLong(2,userId);ps.setLong(3,locationId);ps.setLong(4,statusId("reschedule_request_statuses","PENDING"));ps.setObject(5,asDateTime(a.get("startAt")));ps.setObject(6,asDateTime(a.get("endAt")));ps.setObject(7,requestedStart);ps.setObject(8,requestedStart.plusMinutes(duration));return ps;},k);long req=k.getKey().longValue();jdbc.update("UPDATE professional_slots ps JOIN availability_blocks b ON b.id=ps.availability_block_id SET ps.reschedule_request_id=? WHERE b.professional_id=? AND b.location_id=? AND ps.start_at>=? AND ps.end_at<=? AND ps.appointment_id IS NULL",req,a.get("professionalId"),locationId,(requestedStart),(requestedStart.plusMinutes(duration)));return one("SELECT id,status_id statusId,requested_start_at requestedStartAt,requested_end_at requestedEndAt FROM reschedule_requests WHERE id=?",req);}

    @Transactional
    public void decideReschedule(long adminId,long requestId,boolean approve,String reason){Map<String,Object>r=one("SELECT rr.*,rs.code status,a.professional_id professionalId,a.specialty_id specialtyId FROM reschedule_requests rr JOIN reschedule_request_statuses rs ON rs.id=rr.status_id JOIN appointments a ON a.id=rr.appointment_id WHERE rr.id=?",requestId);if(!"PENDING".equals(r.get("status")))throw new BusinessException("INVALID_TRANSITION","Only pending reschedules can be decided");if(!approve&&(reason==null||reason.isBlank()))throw new BusinessException("REJECTION_REASON_REQUIRED","A rejection requires a reason");if(approve){jdbc.update("UPDATE professional_slots SET appointment_id=NULL WHERE appointment_id=? AND reschedule_request_id IS NULL",r.get("appointment_id"));jdbc.update("UPDATE professional_slots SET appointment_id=?,reschedule_request_id=NULL WHERE reschedule_request_id=?",r.get("appointment_id"),requestId);jdbc.update("UPDATE appointments SET location_id=(SELECT requested_location_id FROM reschedule_requests WHERE id=?),scheduled_start_at=(SELECT requested_start_at FROM reschedule_requests WHERE id=?),scheduled_end_at=(SELECT requested_end_at FROM reschedule_requests WHERE id=?),updated_at=NOW(6) WHERE id=?",requestId,requestId,requestId,r.get("appointment_id"));jdbc.update("UPDATE reschedule_requests SET status_id=?,decision_reason=?,decided_by_user_id=?,decided_at=NOW(6) WHERE id=?",statusId("reschedule_request_statuses","APPROVED"),reason,adminId,requestId);}else{jdbc.update("UPDATE professional_slots SET reschedule_request_id=NULL WHERE reschedule_request_id=?",requestId);jdbc.update("UPDATE reschedule_requests SET status_id=?,decision_reason=?,decided_by_user_id=?,decided_at=NOW(6) WHERE id=?",statusId("reschedule_request_statuses","REJECTED"),reason,adminId,requestId);}}

    public List<Map<String,Object>> adminInbox(){List<Map<String,Object>>out=new ArrayList<>(jdbc.queryForList("SELECT 'APPOINTMENT' itemType,a.id,st.code status,a.scheduled_start_at startAt,s.name specialtyName,l.code locationCode,u.first_name patientFirstName,u.last_name patientLastName FROM appointments a JOIN appointment_statuses st ON st.id=a.status_id JOIN specialties s ON s.id=a.specialty_id JOIN locations l ON l.id=a.location_id JOIN users u ON u.id=a.patient_user_id WHERE st.code='REQUESTED' ORDER BY a.scheduled_start_at"));out.addAll(jdbc.queryForList("SELECT 'RESCHEDULE' itemType,rr.id,rs.code status,rr.requested_start_at startAt,s.name specialtyName,l.code locationCode,u.first_name patientFirstName,u.last_name patientLastName FROM reschedule_requests rr JOIN reschedule_request_statuses rs ON rs.id=rr.status_id JOIN appointments a ON a.id=rr.appointment_id JOIN specialties s ON s.id=a.specialty_id JOIN locations l ON l.id=rr.requested_location_id JOIN users u ON u.id=rr.requested_by_user_id WHERE rs.code='PENDING' ORDER BY rr.requested_start_at"));return out;}

    public List<Map<String,Object>> upcomingReminderAppointments(int hours) {
        int window = Math.max(1, Math.min(hours, 168));
        LocalDateTime now = LocalDateTime.now(clock);
        return jdbc.queryForList("SELECT a.id,a.scheduled_start_at startAt,st.code status,u.email patientEmail "
                + "FROM appointments a JOIN appointment_statuses st ON st.id=a.status_id "
                + "JOIN users u ON u.id=a.patient_user_id "
                + "WHERE st.code='APPROVED' AND a.scheduled_start_at BETWEEN ? AND ? "
                + "ORDER BY a.scheduled_start_at", (now), (now.plusHours(window)));
    }

    private Map<String,Object> ownedAppointment(long userId,long appointmentId){return one("SELECT a.id,a.scheduled_start_at startAt,a.scheduled_end_at endAt,a.professional_id professionalId,a.location_id locationId,a.specialty_id specialtyId,s.appointment_duration_minutes durationMinutes,st.code status FROM appointments a JOIN specialties s ON s.id=a.specialty_id JOIN appointment_statuses st ON st.id=a.status_id WHERE a.id=? AND a.patient_user_id=?",appointmentId,userId);}
    private void transitionAppointment(long id,String status,long actor,String source,String reason){jdbc.update("UPDATE appointments SET status_id=?,updated_at=NOW(6) WHERE id=?",statusId("appointment_statuses",status),id);jdbc.update("INSERT INTO appointment_status_history(appointment_id,status_id,changed_by_user_id,change_source,reason,changed_at) VALUES(?,?,?, ?,?,NOW(6))",id,statusId("appointment_statuses",status),actor,source,reason);}
    private void releaseAppointmentSlots(long id){jdbc.update("UPDATE professional_slots SET appointment_id=NULL WHERE appointment_id=?",id);}
    private void ensureConsecutive(List<Map<String,Object>>slots,LocalDateTime start,int duration){int count=duration/30;if(slots.size()<count)throw new BusinessException("SLOT_UNAVAILABLE","The selected time is no longer available");LocalDateTime expected=start;for(int i=0;i<count;i++){LocalDateTime found=asDateTime(slots.get(i).get("startAt"));if(!expected.equals(found))throw new BusinessException("SLOT_UNAVAILABLE","The selected time does not have consecutive slots");expected=expected.plusMinutes(30);}}
    private LocalDateTime asDateTime(Object value){if(value instanceof LocalDateTime local)return local;if(value instanceof Timestamp timestamp)return timestamp.toLocalDateTime();throw new BusinessException("INVALID_DATETIME","Stored appointment time is invalid");}
    private long professionalId(long userId){List<Map<String,Object>>rows=jdbc.queryForList("SELECT id FROM professionals WHERE user_id=? AND active=TRUE",userId);if(rows.isEmpty())throw new BusinessException("PROFESSIONAL_REQUIRED","An active professional profile is required");return ((Number)rows.get(0).get("id")).longValue();}
    private long locationId(String code){return ((Number)one("SELECT id FROM locations WHERE code=? AND active=TRUE",code).get("id")).longValue();}
    private long statusId(String table,String code){return ((Number)one("SELECT id FROM "+table+" WHERE code=?",code).get("id")).longValue();}
    private Map<String,Object> one(String sql,Object...args){List<Map<String,Object>>rows=jdbc.queryForList(sql,args);if(rows.isEmpty())throw new BusinessException("NOT_FOUND","Requested resource was not found");return new HashMap<>(rows.get(0));}
}
