package com.dercio.database_proxy.restapi;

import com.dercio.database_proxy.common.database.Repository;
import com.dercio.database_proxy.common.mapper.Mapper;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestApiHandlerTest {

    @Mock
    private Repository repository;
    @Mock
    private Mapper mapper;
    @Mock
    private RoutingContext event;
    @Mock
    private HttpServerResponse response;
    @Mock
    private RequestBody requestBody;

    private RestApiHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RestApiHandler(mapper, repository);

        lenient().when(event.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        lenient().when(event.body()).thenReturn(requestBody);
        lenient().when(requestBody.asJsonObject()).thenReturn(new JsonObject());
        lenient().when(event.normalizedPath()).thenReturn("/api/cars/1");
        lenient().when(event.response()).thenReturn(response);

        lenient().when(response.setChunked(anyBoolean())).thenReturn(response);
        lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
        lenient().when(response.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(response);
    }

    @Test
    void getResourcesShouldStreamEncodedRows() {
        when(repository.getData(any())).thenReturn(Future.succeededFuture(java.util.List.of(
                new JsonObject().put("id", 1))));

        handler.getResources(event);

        verify(response).end("[{\"id\":1}]");
    }

    @Test
    void getResourceByIdShouldReturnTheEncodedRowWhenPresent() {
        when(repository.getDataById(any()))
                .thenReturn(Future.succeededFuture(Optional.of(new JsonObject().put("id", 1))));

        handler.getResourceById(event);

        verify(response).end("{\"id\":1}");
        verify(response, never()).setStatusCode(anyInt());
    }

    @Test
    void getResourceByIdShouldReturn404WhenAbsent() {
        when(repository.getDataById(any())).thenReturn(Future.succeededFuture(Optional.empty()));
        when(mapper.encode(any())).thenReturn("{error}");

        handler.getResourceById(event);

        verify(response).setStatusCode(404);
        verify(response).end("{error}");
    }

    @Test
    void createResourceShouldReturn201WithLocationHeaderPointingAtTheNewId() {
        HttpServerRequest request = org.mockito.Mockito.mock(HttpServerRequest.class);
        when(event.request()).thenReturn(request);
        when(request.absoluteURI()).thenReturn("http://localhost/api/cars");
        when(repository.createData(any())).thenReturn(Future.succeededFuture("42"));

        handler.createResource(event);

        verify(response).setStatusCode(201);
        verify(response).putHeader(HttpHeaders.LOCATION, "http://localhost/api/cars/42");
        verify(response).end();
    }

    @Test
    void updateResourceByIdShouldReturn204WhenARowIsUpdated() {
        when(repository.updateData(any())).thenReturn(Future.succeededFuture(1));

        handler.updateResourceById(event);

        verify(response).setStatusCode(204);
        verify(response).end();
    }

    @Test
    void updateResourceByIdShouldReturn404WhenNothingIsUpdated() {
        when(repository.updateData(any())).thenReturn(Future.succeededFuture(0));
        when(mapper.encode(any())).thenReturn("{error}");

        handler.updateResourceById(event);

        verify(response).setStatusCode(404);
        verify(response).end("{error}");
    }

    @Test
    void deleteResourceByIdShouldReturn204WhenARowIsDeleted() {
        when(repository.deleteDataById(any())).thenReturn(Future.succeededFuture(1));

        handler.deleteResourceById(event);

        verify(response).setStatusCode(204);
        verify(response).end();
    }

    @Test
    void deleteResourcesShouldReturn404WhenNothingIsDeleted() {
        when(repository.deleteData(any())).thenReturn(Future.succeededFuture(0));
        when(mapper.encode(any())).thenReturn("{error}");

        handler.deleteResources(event);

        verify(response).setStatusCode(404);
        verify(response).end("{error}");
    }

    @Test
    void shouldFailTheRoutingContextWhenTheRepositoryFails() {
        var cause = new RuntimeException("boom");
        when(repository.getData(any())).thenReturn(Future.failedFuture(cause));

        handler.getResources(event);

        verify(event).fail(cause);
    }
}
