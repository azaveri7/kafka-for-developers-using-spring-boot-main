package util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paathshala.domain.Book;
import com.paathshala.domain.LibraryEvent;
import com.paathshala.domain.LibraryEventType;

public class TestUtil {

    public static Book bookRecord(){

        return new Book(123, "AnandZaveri","Kafka By PaathShala" );
    }

    public static Book bookRecordWithInvalidValues(){

        return new Book(null, "","Kafka Using Spring Boot by Paathshala" );
    }

    public static LibraryEvent libraryEventRecord(){

        return
                new LibraryEvent(null,
                        LibraryEventType.NEW,
                        bookRecord());
    }

    public static LibraryEvent newLibraryEventRecordWithLibraryEventId(){

        return
                new LibraryEvent(123,
                        LibraryEventType.NEW,
                        bookRecord());
    }

    public static LibraryEvent libraryEventRecordUpdate(){

        return
                new LibraryEvent(123,
                        LibraryEventType.UPDATE,
                        bookRecord());
    }

    public static LibraryEvent libraryEventRecordUpdateWithNullLibraryEventId(){

        return
                new LibraryEvent(null,
                        LibraryEventType.UPDATE,
                        bookRecord());
    }

    public static LibraryEvent libraryEventRecordWithInvalidBook(){

        return
                new LibraryEvent(null,
                        LibraryEventType.NEW,
                        bookRecordWithInvalidValues());
    }

    public static LibraryEvent parseLibraryEventRecord(ObjectMapper objectMapper , String json){

        try {
            return  objectMapper.readValue(json, LibraryEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
