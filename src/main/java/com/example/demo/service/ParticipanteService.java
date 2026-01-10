package com.example.demo.service;

import com.example.demo.model.Participante;
import com.example.demo.repository.ParticipanteRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ParticipanteService {

    @Autowired
    private ParticipanteRepository repository;

    @org.springframework.transaction.annotation.Transactional
    // Inner class for result
    public static class ProcessingResult {
        private final List<String> errors;
        private final List<Long> savedIds;

        public ProcessingResult(List<String> errors, List<Long> savedIds) {
            this.errors = errors;
            this.savedIds = savedIds;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<Long> getSavedIds() {
            return savedIds;
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public ProcessingResult processExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        List<Participante> participantes = new ArrayList<>();
        List<Long> savedIds = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            System.out.println("Processing Excel. Total rows found (0-indexed): " + lastRow);

            if (lastRow < 1) {
                errors.add("El archivo Excel no contiene datos (solo cabeceras o vacío).");
                return new ProcessingResult(errors, savedIds);
            }

            // Start from row 2 (index 1)
            for (int i = 1; i <= lastRow; i++) {
                // ... (Parsing logic remains the same, omitted for brevity in tool call context
                // if not verifying)
                // Need to re-include the loop logic to avoid deleting it.
                // Since replace_file_content replaces a block, I must include the whole method
                // body or carefully target chunks.
                // Given the size, let's rewrite the method body carefully.
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                String nombres = dataFormatter.formatCellValue(row.getCell(0));
                String apellidos = dataFormatter.formatCellValue(row.getCell(1));

                Cell dniCell = row.getCell(2);
                String dni = "";
                if (dniCell != null && dniCell.getCellType() == CellType.NUMERIC) {
                    dni = String.format("%.0f", dniCell.getNumericCellValue());
                } else {
                    dni = dataFormatter.formatCellValue(dniCell);
                }

                Date fechaInicioRaw = getDateCellValue(row, 3);
                Date fechaFinalRaw = getDateCellValue(row, 4);
                Integer horas = getIntegerCellValue(row, 5);
                String rol = dataFormatter.formatCellValue(row.getCell(6));
                String programa = dataFormatter.formatCellValue(row.getCell(7));
                String equipoValor = dataFormatter.formatCellValue(row.getCell(8));

                if (isEmpty(equipoValor)) {
                    equipoValor = "NINGUNO";
                }

                // Logging
                System.out.println("Reading Row " + i + ": " + nombres + " " + apellidos + " DNI:" + dni);

                StringBuilder rowError = new StringBuilder();
                if (isEmpty(nombres))
                    rowError.append("Nombres es obligatorio. ");
                if (isEmpty(apellidos))
                    rowError.append("Apellidos es obligatorio. ");

                if (isEmpty(dni)) {
                    rowError.append("DNI es obligatorio. ");
                } else {
                    if (!dni.matches("\\d{8}") && !dni.matches("\\d{11}")) {
                        rowError.append("DNI debe tener 8 o 11 dígitos. Valor: '" + dni + "'. ");
                    }
                }

                if (fechaInicioRaw == null)
                    rowError.append("Fecha Inicio es inválida o vacía. ");
                if (fechaFinalRaw == null)
                    rowError.append("Fecha Final es inválida o vacía. ");
                if (horas == null)
                    rowError.append("Horas es inválido o vacío. ");
                if (isEmpty(rol))
                    rowError.append("Rol es obligatorio. ");
                if (isEmpty(programa))
                    rowError.append("Programa es obligatorio. ");

                if (rowError.length() > 0) {
                    String msg = "Fila " + (i + 1) + ": " + rowError.toString();
                    errors.add(msg);
                    System.out.println("Validation Error: " + msg);
                    continue;
                }

                Participante p = new Participante();
                p.setNombres(nombres);
                p.setApellidos(apellidos);
                p.setDni(dni);
                p.setFechaInicio(fechaInicioRaw.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                p.setFechaFinal(fechaFinalRaw.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                p.setHoras(horas);
                p.setRol(rol);
                p.setPrograma(programa);
                p.setEquipoValor(equipoValor);
                p.setCodigoVerificacion(generateUniqueCode());

                participantes.add(p);
            }

            if (errors.isEmpty()) {
                if (participantes.isEmpty()) {
                    errors.add("No se encontraron participantes válidos para guardar.");
                } else {
                    int count = 0;
                    for (Participante p : participantes) {
                        try {
                            Participante saved = repository.save(p);
                            savedIds.add(saved.getId());
                            count++;
                            System.out.println("Guardando participante en BD: " + p.getNombres() + " "
                                    + p.getApellidos() + " (CODE: " + p.getCodigoVerificacion() + ")");
                        } catch (Exception e) {
                            System.err.println("Error al guardar participante: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    System.out.println("Proceso finalizado. Total guardados: " + count);
                }
            } else {
                System.out.println("No se guardó nada debido a errores de validación.");
                for (String err : errors) {
                    System.out.println(" > " + err);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            errors.add("Error al procesar el archivo: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            errors.add("Error inesperado: " + e.getMessage());
        }

        return new ProcessingResult(errors, savedIds);
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = "CERT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (repository.existsByCodigoVerificacion(code));
        return code;
    }

    private Date getDateCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() == CellType.BLANK)
            return null;
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue();
        }
        return null;
    }

    private Integer getIntegerCellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null)
            return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Integer.parseInt(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    // Helper to check if row is visually empty
    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
