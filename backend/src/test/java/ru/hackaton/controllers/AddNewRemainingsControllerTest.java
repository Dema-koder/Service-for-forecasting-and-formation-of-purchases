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
import ru.hackaton.parsers.StockRemainingsParser;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AddNewRemainingsController.class)
class AddNewRemainingsControllerTest {
    @MockBean
    private StockRemainingsParser parser;
    @MockBean
    private FromMultipartToFile fromMultipartToFile;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void uploadXlsx_success() throws Exception {
        File tempFile = new File("testfile", ".xlsx");
        FileUtils.writeStringToFile(tempFile, "Test data");
        MockMultipartFile file = new MockMultipartFile("file", "testfile.xlsx", "application/vnd.ms-excel", Files.readAllBytes(tempFile.toPath()));

        when(fromMultipartToFile.convertMultipartFileToFile(any(MultipartFile.class))).thenReturn(tempFile);
        doNothing().when(parser).processFile(any(File.class));

        String expected = "Success Upload";
        mockMvc.perform(MockMvcRequestBuilders.multipart("/upload/remainings")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(expected));
        assertFalse(tempFile.exists());
    }

    @Test
    public void turnoversUpload_fail() throws Exception {
        File tempFile = null;
        MockMultipartFile file = new MockMultipartFile("file", "testfile.xlsx", "application/vnd.ms-excel", "to".getBytes());
        String expected = "Upload Fail";
        when(fromMultipartToFile.convertMultipartFileToFile(any(MultipartFile.class))).thenReturn(tempFile);
        doNothing().when(parser).processFile(any(File.class));
        //doThrow(new IOException()).when(tempFile).delete();

        mockMvc.perform(MockMvcRequestBuilders.multipart("/upload/remainings")
                        .file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value(expected));
    }
}