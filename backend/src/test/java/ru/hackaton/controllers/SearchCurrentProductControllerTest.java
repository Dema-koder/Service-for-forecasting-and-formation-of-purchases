package ru.hackaton.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.hackaton.service.SimilarProductFromDB;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchCurrentProductController.class)
class SearchCurrentProductControllerTest {
    @MockBean
    private SimilarProductFromDB similarProductFromDB;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void currentProduct_success() throws Exception{
        List<String>expected = new ArrayList<>();
        expected.add("test1");
        when(similarProductFromDB.mostSimilarProduct(any(String.class))).thenReturn(expected);

        mockMvc.perform(get("/search-product")
                    .param("product", "product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(expected));
        verify(similarProductFromDB, times(1)).mostSimilarProduct(any(String.class));
    }

    @Test
    public void currentProduct_failed() throws Exception{
        List<String>expected = new ArrayList<>();
        when(similarProductFromDB.mostSimilarProduct(any(String.class))).thenReturn(expected);

        mockMvc.perform(get("/search-product")
                        .param("product", "product"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value(expected));
        verify(similarProductFromDB, times(1)).mostSimilarProduct(any(String.class));
    }
}