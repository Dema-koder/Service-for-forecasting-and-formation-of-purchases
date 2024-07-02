package ru.hackaton.controllers;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;
import ru.hackaton.parsers.FromMultipartToFile;
import ru.hackaton.parsers.TurnoversParser;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TurnoversController.class)
class TurnoversControllerTest {
    @MockBean
    private FromMultipartToFile fromMultipartToFile;
    @MockBean
    private TurnoversParser parser;

    @Autowired
    private MockMvc mockMvc;

    File tempFile;
    MockMultipartFile file;

    @Test
    public void turnoversUpload_success() throws Exception {
        tempFile = new File("testfile", ".xlsx");
        FileUtils.writeStringToFile(tempFile, "Test data");
        file = new MockMultipartFile("file", "testfile.xlsx", "application/vnd.ms-excel", Files.readAllBytes(tempFile.toPath()));

        String expected = "Success Upload";
        when(fromMultipartToFile.convertMultipartFileToFile(any(MultipartFile.class))).thenReturn(tempFile);
        doNothing().when(parser).processFile(any(File.class));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/upload/turnovers")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(expected));
        verify(parser, times(1)).processFile(tempFile);
        assertFalse(tempFile.exists());
    }

    @Test
    public void turnoversUpload_fail() throws Exception {
        tempFile = null;
        file = new MockMultipartFile("file", "testfile.xlsx", "application/vnd.ms-excel", "to".getBytes());
        String expected = "Fail To Upload";
        when(fromMultipartToFile.convertMultipartFileToFile(any(MultipartFile.class))).thenReturn(tempFile);
        doNothing().when(parser).processFile(any(File.class));
        //doThrow(new IOException()).when(tempFile).delete();

        mockMvc.perform(MockMvcRequestBuilders.multipart("/upload/turnovers")
                        .file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value(expected));
    }
}