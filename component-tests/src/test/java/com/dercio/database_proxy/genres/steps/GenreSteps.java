package com.dercio.database_proxy.genres.steps;

import com.dercio.database_proxy.genres.Genre;
import com.dercio.database_proxy.genres.GenreRepository;
import com.dercio.database_proxy.genres.GenreService;
import com.google.inject.Inject;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Every column of the genres table is part of the primary key, so an update has nothing to SET —
 * CQL cannot assign a key column to itself the way the Postgres implementation does.
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GenreSteps {

    private final GenreContext genreContext;
    private final GenreRepository genreRepository;
    private final GenreService genreService;

    @Given("a list of genres exists")
    public void aListOfGenresExists() {
        List.of(new Genre("bebop"), new Genre("hard bop"))
                .forEach(genre -> {
                    genreContext.getGenres().add(genre);
                    genreRepository.save(genre);
                });
    }

    @When("I retrieve the genre {string}")
    public void iRetrieveTheGenre(String name) {
        genreContext.setResponse(genreService.getById(name));
    }

    @Then("I should see the genre {string}")
    public void iShouldSeeTheGenre(String name) {
        genreContext.getResponse().then().statusCode(200).body("name", equalTo(name));
    }

    @When("I update the genre {string}")
    public void iUpdateTheGenre(String name) {
        genreContext.setResponse(genreService.update(name, new Genre(name)));
    }

    @Then("the genre should still exist")
    public void theGenreShouldStillExist() {
        assertEquals(204, genreContext.getResponse().statusCode());
        assertNotNull(genreRepository.findByName("bebop"));
    }

    @When("I update a genre that does not exist")
    public void iUpdateAGenreThatDoesNotExist() {
        genreContext.setResponse(genreService.update("not-a-genre", new Genre("not-a-genre")));
    }

    @Then("the genre should not be created by the update")
    public void theGenreShouldNotBeCreatedByTheUpdate() {
        assertEquals(404, genreContext.getResponse().statusCode());
        assertNull(genreRepository.findByName("not-a-genre"));
    }

    @When("I delete all the genres")
    public void iDeleteAllTheGenres() {
        genreContext.setResponse(genreService.delete(Map.of()));
    }

    @Then("no genres should remain")
    public void noGenresShouldRemain() {
        // A collection delete becomes a TRUNCATE, which reports no row count and so always answers
        // 204 - unlike Postgres, an already-empty table is not a 404 here.
        assertEquals(204, genreContext.getResponse().statusCode());
        assertTrue(genreRepository.find().isEmpty());
    }
}
