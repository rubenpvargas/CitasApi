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



    public List<Map<String,Object>> professionalAgenda(long userId,LocalDate from,LocalDate to,String locationCode){long p=professionalId(userId);String extra=locationCode==null?"":" AND l.code=?";return locationCode==null?jdbc.queryForList("SELECT a.id,a.scheduled_start_at startAt,a.scheduled_end_at endAt,st.code status,s.name specialtyName,l.code locationCode,u.first_name patientFirstName,u.last_name patientLastName FROM appointments a JOIN appointment_statuses st ON st.id=a.status_id JOIN specialties s ON s.id=a.specialty_id JOIN locations l ON l.id=a.location_id JOIN users u ON u.id=a.patient_user_id WHERE a.professional_id=? AND st.code='APPROVED' AND DATE(a.scheduled_start_at) BETWEEN ? AND ? ORDER BY a.scheduled_start_at",p,from,to):jdbc.queryForList("SELECT a.id,a.scheduled_start_at startAt,a.scheduled_end_at endAt,st.code status,s.name specialtyName,l.code locationCode,u.first_name patientFirstName,u.last_name patientLastName FROM appointments a JOIN appointment_statuses st ON st.id=a.status_id JOIN specialties s ON s.id=a.specialty_id JOIN locations l ON l.id=a.location_id JOIN users u ON u.id=a.patient_user_id WHERE a.professional_id=? AND st.code='APPROVED' AND DATE(a.scheduled_start_at) BETWEEN ? AND ?"+extra+" ORDER BY a.scheduled_start_at",p,from,to,locationCode);}

    @Transactional
    public void closeAppointment(long userId,long appointmentId,String outcome){long p=professionalId(userId);Map<String,Object>a=one("SELECT a.id,a.scheduled_start_at startAt,st.code status FROM appointments a JOIN appointment_statuses st ON st.id=a.status_id WHERE a.id=? AND a.professional_id=?",appointmentId,p);if(!"APPROVED".equals(a.get("status")))throw new BusinessException("INVALID_TRANSITION","Only approved appointments can be closed");if(asDateTime(a.get("startAt")).isAfter(LocalDateTime.now(clock)))throw new BusinessException("APPOINTMENT_NOT_APPLICABLE","The appointment is not applicable yet");if(!List.of("COMPLETED","NO_SHOW").contains(outcome))throw new BusinessException("INVALID_OUTCOME","Outcome must be COMPLETED or NO_SHOW");transitionAppointment(appointmentId,outcome,userId,"USER","Professional closed appointment");}



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
