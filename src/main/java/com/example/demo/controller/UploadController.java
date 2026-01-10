package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UploadController {

    @GetMapping("/upload")
    public String showUploadPage() {
        return "upload";
    }

    @Autowired
    private com.example.demo.service.ParticipanteService participanteService;

    @Autowired
    private com.example.demo.service.CertificateService certificateService;

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes,
            jakarta.servlet.http.HttpSession session) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Por favor selecciona un archivo.");
            return "redirect:/upload";
        }

        com.example.demo.service.ParticipanteService.ProcessingResult result = participanteService.processExcel(file);
        List<String> errors = result.getErrors();

        if (errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("message",
                    "Archivo procesado y guardado exitosamente. (" + result.getSavedIds().size() + " registros)");
            // Update logic: set session variable with new batch
            session.setAttribute("lastUploadedIds", result.getSavedIds());
        } else {
            redirectAttributes.addFlashAttribute("errorList", errors);
        }

        return "redirect:/upload";
    }

    @GetMapping("/download-certificates")
    public org.springframework.http.ResponseEntity<byte[]> downloadCertificates(
            jakarta.servlet.http.HttpSession session) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) session.getAttribute("lastUploadedIds");

            if (ids == null || ids.isEmpty()) {
                // Nothing to download
                return org.springframework.http.ResponseEntity.notFound().build();
            }

            byte[] zipBytes = certificateService.generateCertificatesZip(ids);

            return org.springframework.http.ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"certificados_lote.zip\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .body(zipBytes);
        } catch (java.io.IOException e) {
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }
}
