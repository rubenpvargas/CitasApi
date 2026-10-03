package com.fcv.citas.application.model;

/** Campos editables del perfil en v1; email y documento no son editables. */
public record ProfileUpdateCommand(String firstName, String lastName, String phone) {
}
