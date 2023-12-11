package com.dercio.database_proxy.glue;

import com.dercio.database_proxy.cars.steps.CarsContext;
import com.dercio.database_proxy.cars.CarsRepository;
import com.dercio.database_proxy.students.StudentRepository;
import com.dercio.database_proxy.students.steps.StudentContext;
import com.dercio.database_proxy.wheel.WheelsRepository;
import com.dercio.database_proxy.wheel.steps.WheelsContext;
import com.google.inject.Inject;
import io.cucumber.java.After;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.mybatis.guice.transactional.Transactional;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CockroachHook {

    private final CarsContext carsContext;
    private final CarsRepository carsRepository;

    private final WheelsContext wheelsContext;
    private final WheelsRepository wheelsRepository;

    private final StudentContext studentContext;
    private final StudentRepository studentRepository;

    @Transactional
    @After("@cockroach")
    public void afterScenario() {
        log.info("Cleaning up scenario");

        log.info("Deleting {} cars from scenario", carsContext.getCars().size());
        carsContext.getCars().forEach(car -> carsRepository.deleteById(car.getCarId()));

        log.info("Deleting {} wheels from scenario", wheelsContext.getWheels().size());
        wheelsContext.getWheels().forEach(wheel -> wheelsRepository.deleteByType(wheel.getWheelType().toString()));

        log.info("Deleting {} students from scenario", studentContext.getStudents().size());
        studentContext.getStudents().forEach(student -> studentRepository.deleteByNameAndAge(student.name(), student.age()));
    }
}
