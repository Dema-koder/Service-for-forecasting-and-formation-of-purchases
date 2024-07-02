package ru.hackaton.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.hackaton.parsers.FZ44Parser;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UpdateInLowController.class)
class UpdateInLowControllerTest {
    @MockBean
    private FZ44Parser fz44Parser;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getLowUpdate_success() throws Exception {
        String expected = "2024-06-07";
        when(fz44Parser.isUpdate()).thenReturn(expected);
        mockMvc.perform(get("/law-update"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(expected));
        verify(fz44Parser, times(1)).isUpdate();
    }

    @Test
    public void getLowUpdate_fail() throws Exception {
        String date = "2024.06.07";
        String expected = "No updates in law";
        when(fz44Parser.isUpdate()).thenReturn(date);
        mockMvc.perform(get("/law-update"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value(expected));
        verify(fz44Parser, times(1)).isUpdate();
    }
}