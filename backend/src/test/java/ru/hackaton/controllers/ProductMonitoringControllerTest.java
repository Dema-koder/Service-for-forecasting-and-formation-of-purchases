package ru.hackaton.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.hackaton.service.ProductMonitoringDB;

import java.util.ArrayList;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductMonitoringController.class)
class ProductMonitoringControllerTest {
    @MockBean
    private ProductMonitoringDB db;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void addProduct_success() throws Exception {
        String expected = "Success";
        long userId = 1L;
        String product = "product";
        when(db.addToMonitoringDB(userId, product)).thenReturn(expected);

        mockMvc.perform(post("/monitoring/add")
                        .param("user_id", Long.toString(userId))
                        .param("product", product))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(expected));
        verify(db, times(1)).addToMonitoringDB(userId, product);
    }

    @Test
    public void addProduct_failed() throws Exception {
        String expected = "Fail";
        when(db.addToMonitoringDB(any(Long.class), any(String.class))).thenReturn(expected);

        mockMvc.perform(post("/monitoring/add")
                        .param("user_id", Long.toString(1L))
                        .param("product", "product"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value(expected));
        verify(db, times(1)).addToMonitoringDB(any(Long.class), any(String.class));
    }

    @Test
    public void removeProduct_success() throws Exception {
        String expected = "Success";
        long userId = 1L;
        String product = "product";
        when(db.removeFromMonitoringDB(userId, product)).thenReturn(expected);

        mockMvc.perform(delete("/monitoring/delete")
                        .param("user_id", Long.toString(userId))
                        .param("product", product))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(expected));
        verify(db, times(1)).removeFromMonitoringDB(userId, product);
    }

    @Test
    public void removeProduct_failed() throws Exception {
        String expected = "Fail";
        when(db.removeFromMonitoringDB(any(Long.class), any(String.class))).thenReturn(expected);

        mockMvc.perform(delete("/monitoring/delete")
                        .param("user_id", Long.toString(1L))
                        .param("product", "product"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value(expected));
        verify(db, times(1)).removeFromMonitoringDB(any(Long.class), any(String.class));
    }

    @Test
    public void allProduct() throws Exception {
        ArrayList<String> expected = new ArrayList<>();
        expected.add("test1");
        expected.add("test2");
        when(db.allProductForSpecialUser(any(Long.class))).thenReturn(expected);

        mockMvc.perform(get("/monitoring/all")
                    .param("user_id", Long.toString(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(expected));
        verify(db, times(1)).allProductForSpecialUser(any(Long.class));
    }

    @Test
    public void scheduleRequest_success() throws Exception {
        ArrayList<String> expected = new ArrayList<>();
        expected.add("test1");
        expected.add("test2");
        when(db.scheduleRequest(any(Long.class))).thenReturn(expected);

        mockMvc.perform(get("/monitoring/schedule")
                    .param("user_id", Long.toString(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(expected));
        verify(db, times(1)).scheduleRequest(any(Long.class));
    }

    @Test
    public void scheduleRequest_failed() throws Exception {
        ArrayList<String> expected = new ArrayList<>();
        when(db.scheduleRequest(any(Long.class))).thenReturn(expected);

        mockMvc.perform(get("/monitoring/schedule")
                        .param("user_id", Long.toString(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value(expected));
        verify(db, times(1)).scheduleRequest(any(Long.class));
    }
}