package com.fcv.citas.application.port.in;

import com.fcv.citas.application.model.AdminInbox;
import com.fcv.citas.application.model.InboxFilter;

/** HU-025 — bandeja ADMIN de pendientes. */
public interface AdminInboxUseCase {
    AdminInbox inbox(InboxFilter filter);
}
