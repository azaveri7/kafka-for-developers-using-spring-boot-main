package com.paathshala.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paathshala.domain.LibraryEvent;
import com.paathshala.producer.LibraryEventProducer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import util.TestUtil;

// Test slice
@WebMvcTest(LibraryEventsController.class)
class LibraryEventsControllerUnitTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    LibraryEventProducer libraryEventProducer;

    @Test
    void postLibraryEvent() throws Exception {
        //given
        var json = objectMapper.writeValueAsString(TestUtil.libraryEventRecord());
        Mockito.when(libraryEventProducer
                .sendLibraryEvent_Approach2(ArgumentMatchers.isA(LibraryEvent.class)))
                        .thenReturn(null);
        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/libraryevent")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated());

        //then
    }

    @Test
    void postLibraryEvent_4xx() throws Exception {
        //given
        var json = objectMapper.writeValueAsString(TestUtil.libraryEventRecordWithInvalidBook());
        Mockito.when(libraryEventProducer
                        .sendLibraryEvent_Approach2(ArgumentMatchers.isA(LibraryEvent.class)))
                .thenReturn(null);
        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/libraryevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());

        //then
    }
}