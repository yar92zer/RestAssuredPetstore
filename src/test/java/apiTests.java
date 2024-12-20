import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class apiTests {
    private static final int NONEXISTENT_PET_ID = 19;
    private static final int NEW_PET_ID = 11;
    private static final int MY_PET_ID = 20;
    private static final String BASE_PATH = "pet/";

    @BeforeAll
    public static void setUpRestAssured() {
        RestAssured.baseURI = "https://petstore.swagger.io/v2/";
        RestAssured.requestSpecification = given().contentType("application/json");
    }

    @BeforeEach
    public void setUp() {
        deletePetIfExists(NONEXISTENT_PET_ID);
        deletePetIfExists(NEW_PET_ID);
        deletePetIfExists(MY_PET_ID);
    }

    @Test
    public void petNotFoundTestWithAssert() {
        Response response = RestAssured.requestSpecification.when().get(BASE_PATH + NONEXISTENT_PET_ID);
        System.out.println("Response: " + response.asPrettyString());
        assertEquals(404, response.statusCode(), "Некорректный статус код");
        assertEquals("HTTP/1.1 404 Not Found", response.statusLine(), "Некорректная статус линия");
        assertEquals("Pet not found", response.jsonPath().get("message"), "Некорректное сообщение об ошибке");
    }

    @Test
    public void petNotFoundTest() {
        getValidaTableResponse(NONEXISTENT_PET_ID, 404)
                .statusLine("HTTP/1.1 404 Not Found")
                .body("message", equalTo("Pet not found"));
    }

    @Test
    public void petFoundTestBdd() {
        createPet(NONEXISTENT_PET_ID, "Test Pet", "available");
        getValidaTableResponse(NONEXISTENT_PET_ID, 200)
                .body("name", equalTo("Test Pet"))
                .body("status", equalTo("available"));
    }

    @Test
    public void newPetTest() {
        createPet(NEW_PET_ID, "dogg", "sold");
        getValidaTableResponse(NEW_PET_ID, 200)
                .body("name", equalTo("dogg"))
                .body("status", equalTo("sold"));
    }

    private ValidatableResponse getValidaTableResponse(int petId, int statusCode) {
        return given()
                .when()
                .get(BASE_PATH + petId)
                .then()
                .log().all()
                .statusCode(statusCode);
    }

    @Test
    @DisplayName("Тест проверяет создание питомца с фотографией")
    public void createPetWithPhotoTest() {
        String name = "Marti Cat";
        String status = "stock";
        String photoUrl = "https://example.com/photo.jpg";

        Map<String, Object> request = new HashMap<>();
        request.put("id", MY_PET_ID);
        request.put("name", name);
        request.put("status", status);
        request.put("photoUrls", Collections.singletonList(photoUrl));

        RestAssured.requestSpecification
                .body(request)
                .when()
                .post(BASE_PATH)
                .then()
                .log().all()
                .time(lessThan(3000L))
                .statusCode(200)
                .body("id", equalTo(MY_PET_ID))
                .body("name", equalTo(name))
                .body("status", equalTo(status))
                .body("photoUrls[0]", equalTo(photoUrl));
    }

    private void deletePetIfExists(int petId) {
        Response response = RestAssured.requestSpecification.when().delete(BASE_PATH + petId);
        if (response.statusCode() != 404) {
            System.out.println("Deleted pet with ID: " + petId);
        }
    }

    private void createPet(int petId, String name, String status) {
        Map<String, Object> request = new HashMap<>();
        request.put("id", petId);
        request.put("name", name);
        request.put("status", status);

        RestAssured.requestSpecification.body(request)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(200);
    }
}
