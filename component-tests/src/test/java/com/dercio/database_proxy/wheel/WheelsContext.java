package com.dercio.database_proxy.wheel;

import com.dercio.database_proxy.repositories.wheel.Wheel;
import io.cucumber.guice.ScenarioScoped;
import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ScenarioScoped
public class WheelsContext {
    private final List<Wheel> wheels = new ArrayList<>();
    private Response response;
}
