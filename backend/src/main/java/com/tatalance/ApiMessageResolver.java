package com.tatalance;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * Resolves API error messages in English or Spanish based on {@code Accept-Language}.
 */
public final class ApiMessageResolver {

    private static final Map<String, String> KNOWN_ES = Map.ofEntries(
            Map.entry("Pickup date/time cannot be in the past",
                    "La fecha/hora de recogida no puede ser en el pasado"),
            Map.entry("Profile not found or does not belong to user",
                    "Perfil no encontrado — cambia a \"Todos (cuenta)\" en el menú e intenta de nuevo."),
            Map.entry("Client not found",
                    "Cliente no encontrado — actualiza la lista e intenta de nuevo."),
            Map.entry("Ride not found", "Viaje no encontrado"),
            Map.entry("Driver not found", "Chofer no encontrado"),
            Map.entry("driverId is required", "El ID del chofer es obligatorio"),
            Map.entry("Driver is not available", "El chofer no está disponible"),
            Map.entry("Only SCHEDULED rides can be edited",
                    "Solo se pueden editar viajes programados"),
            Map.entry("A client with this phone number already exists",
                    "Ya existe un cliente con este número de teléfono"),
            Map.entry("Invalid request body", "Cuerpo de solicitud inválido"),
            Map.entry("Payout type must be one of: PERCENTAGE, FLAT",
                    "El tipo de pago debe ser PERCENTAGE o FLAT"),
            Map.entry("Availability must be one of: AVAILABLE, ON_TRIP, OFF_DUTY",
                    "La disponibilidad debe ser AVAILABLE, ON_TRIP u OFF_DUTY"),
            Map.entry("Payment method must be one of: CASH, CARD, TRANSFER, ZELLE, VENMO, CHECK",
                    "El método de pago debe ser CASH, CARD, TRANSFER, ZELLE, VENMO o CHECK"),
            Map.entry("rideId is required", "El ID del viaje es obligatorio"),
            Map.entry("Ride must be COMPLETED to generate invoice",
                    "El viaje debe estar COMPLETADO para generar la factura"),
            Map.entry("Invoice not found", "Factura no encontrada"),
            Map.entry("Profile not found", "Perfil no encontrado"),
            Map.entry("Profile type is required", "El tipo de perfil es obligatorio"),
            Map.entry("Phone must have 10 digits, or 11 digits starting with 1 (e.g. 2223334444 or +12223334444)",
                    "El teléfono debe tener 10 dígitos, u 11 dígitos que comiencen con 1 (ej. 2223334444 o +12223334444)")
    );

    private ApiMessageResolver() {
    }

    public static boolean wantsSpanish(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String lang = request.getHeader("Accept-Language");
        if (lang == null || lang.isBlank()) {
            return false;
        }
        String primary = lang.split(",")[0].trim().toLowerCase();
        return primary.startsWith("es");
    }

    public static String fieldMessage(String field, String defaultMsg, boolean spanish) {
        if (defaultMsg != null && defaultMsg.contains("E.164")) {
            return spanish
                    ? "El teléfono debe comenzar con + seguido de 10-15 dígitos (ej. +13055551234)"
                    : "Phone must start with + followed by 10-15 digits (e.g. +13055551234)";
        }
        if (spanish) {
            return spanishFieldMessage(field, defaultMsg);
        }
        return englishFieldMessage(field, defaultMsg);
    }

    public static String translate(String message, boolean spanish) {
        if (!spanish || message == null || message.isBlank()) {
            return message;
        }
        String exact = KNOWN_ES.get(message);
        if (exact != null) {
            return exact;
        }
        if (message.contains("Pickup date/time cannot be in the past")
                || message.contains("cannot be in the past")) {
            return "La fecha/hora de recogida no puede ser en el pasado";
        }
        if (message.startsWith("Cannot cancel a ") && message.endsWith(" ride")) {
            return "No se puede cancelar un viaje " + message.substring("Cannot cancel a ".length());
        }
        if (message.startsWith("Cannot delete client with active rides (")) {
            return message.replace("Cannot delete client with active rides",
                    "No se puede eliminar el cliente con viajes activos")
                    .replace(" active)", " activos)");
        }
        return message;
    }

    private static String englishFieldMessage(String field, String defaultMsg) {
        return switch (field) {
            case "firstName" -> "First name is required";
            case "lastName" -> "Last name is required";
            case "phone" -> "Phone number is required";
            case "clientId" -> "Client is required";
            case "pickupLocation" -> "Pickup location is required";
            case "dropoffLocation" -> "Dropoff location is required";
            case "pickupDateTime" -> "Pickup date/time is required";
            case "payoutType" -> "Payout type is required (PERCENTAGE or FLAT)";
            case "payoutRate" -> "Payout rate is required";
            case "actualStart" -> "Actual start time is required";
            case "actualEnd" -> "Actual end time is required";
            case "amount" -> "Payment amount is required and must be greater than 0";
            case "method" -> "Payment method is required (CASH, CARD, TRANSFER, ZELLE, VENMO, CHECK)";
            case "date" -> "Payment date is required";
            default -> defaultMsg != null ? defaultMsg : field + " is required";
        };
    }

    private static String spanishFieldMessage(String field, String defaultMsg) {
        return switch (field) {
            case "firstName" -> "El nombre es obligatorio";
            case "lastName" -> "El apellido es obligatorio";
            case "phone" -> "El número de teléfono es obligatorio";
            case "clientId" -> "El cliente es obligatorio";
            case "pickupLocation" -> "El lugar de recogida es obligatorio";
            case "dropoffLocation" -> "El lugar de destino es obligatorio";
            case "pickupDateTime" -> "La fecha/hora de recogida es obligatoria";
            case "payoutType" -> "El tipo de pago es obligatorio (PERCENTAGE o FLAT)";
            case "payoutRate" -> "La tasa de pago es obligatoria";
            case "actualStart" -> "La hora de inicio real es obligatoria";
            case "actualEnd" -> "La hora de fin real es obligatoria";
            case "amount" -> "El monto del pago es obligatorio y debe ser mayor que 0";
            case "method" -> "El método de pago es obligatorio (CASH, CARD, TRANSFER, ZELLE, VENMO, CHECK)";
            case "date" -> "La fecha de pago es obligatoria";
            default -> defaultMsg != null ? defaultMsg : field + " es obligatorio";
        };
    }
}