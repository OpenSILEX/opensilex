package org.opensilex.nosql.mongodb.dao;

import org.junit.*;
import org.opensilex.nosql.EmbedMongoClient;
import org.opensilex.nosql.MongoDBServiceTest;

import java.io.IOException;

import static org.junit.Assert.*;

public class MongoReadDaoTest extends MongoDBServiceTest {

    @BeforeClass
    public static void setUp() {
        MongoDBServiceTest.setUp();
    }

    @AfterClass
    public static void stop() {
        MongoDBServiceTest.stop();
    }

    @Test
    public void get() {
    }

    @Test
    public void getWithSession() {
    }

    @Test
    public void exists() {
    }

    @Test
    public void existsWithSession() {
    }

    @Test
    public void count() {
    }

    @Test
    public void countWithSession() {
    }

    @Test
    public void search() {
    }

    @Test
    public void searchWithSession() {
    }

    @Test
    public void searchWithProjection() {
    }

    @Test
    public void searchWithConversion() {
    }

    @Test
    public void searchAsStream() {
    }
    @Test
    public void searchAsStreamWithSession() {
    }

    @Test
    public void searchAsStreamWithProjection() {
    }

    @Test
    public void distinctUris() {
    }

    @Test
    public void distinctUrisWithSession() {
    }

    @Test
    public void distinct() {
    }

    @Test
    public void distinctAggregation() {
    }

    @Test
    public void lookupAggregation() {
    }
}