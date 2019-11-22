--
-- PostgreSQL database dump
--

-- Dumped from database version 9.5.14
-- Dumped by pg_dump version 9.5.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry, geography, and raster spatial types and functions';


--
-- Name: project_users_relation_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.project_users_relation_type AS ENUM (
    'scientific_contact',
    'administrative_contact',
    'project_coordinator'
);


--
-- Name: trial_users_relation_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.trial_users_relation_type AS ENUM (
    'scientific_supervisor',
    'technical_supervisor'
);


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: agronomical_object; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.agronomical_object (
    uri character varying(255) NOT NULL,
    type character varying(100) NOT NULL,
    geometry public.geometry(Geometry,4326) NOT NULL,
    named_graph character varying(255)
);


--
-- Name: at_group_trial; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.at_group_trial (
    trial_uri character varying(255) NOT NULL,
    group_uri character varying(200) NOT NULL
);


--
-- Name: at_group_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.at_group_users (
    users_email character varying(255) NOT NULL,
    group_uri character varying(200) NOT NULL
);


--
-- Name: at_project_project; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.at_project_project (
    uri_project character varying(300) NOT NULL,
    uri_parent_project character varying(300) NOT NULL
);


--
-- Name: at_project_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.at_project_users (
    project_uri character varying(300) NOT NULL,
    users_email character varying(255) NOT NULL,
    type character varying(300) NOT NULL
);


--
-- Name: at_trial_project; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.at_trial_project (
    project_uri character varying(300) NOT NULL,
    trial_uri character varying(256) NOT NULL
);


--
-- Name: at_trial_users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.at_trial_users (
    trial_uri character varying(300) NOT NULL,
    users_email character varying(255) NOT NULL,
    type character varying(300) NOT NULL
);


--
-- Name: group; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public."group" (
    name character varying(200) NOT NULL,
    level character varying(200),
    description text,
    uri character varying(200) NOT NULL
);


--
-- Name: project; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project (
    uri character varying(300) NOT NULL,
    name character varying(200) NOT NULL,
    acronyme character varying(200),
    subproject_type character varying(200),
    financial_support character varying(200),
    financial_name character varying(200),
    date_start date NOT NULL,
    date_end date,
    keywords character varying(500),
    description text,
    objective character varying(256),
    parent_project character varying(300),
    website character varying(300),
    type character varying(100)
);


--
-- Name: session; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.session (
    id character varying(256) NOT NULL,
    email character varying(255),
    date timestamp without time zone,
    date_end timestamp without time zone
);


--
-- Name: trial; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.trial (
    uri character varying(255) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    field character varying(50),
    campaign character varying(50),
    place character varying(50),
    alias character varying(255),
    comment text,
    keywords character varying(255),
    objective character varying(255),
    crop_species character varying(200)
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    email character varying(255) NOT NULL,
    first_name character varying(50),
    family_name character varying(50),
    address character varying(255),
    password character varying(255),
    available boolean DEFAULT true NOT NULL,
    phone character varying(255),
    orcid character varying(255),
    affiliation character varying(255) NOT NULL,
    isadmin boolean DEFAULT false NOT NULL,
    uri character varying(255)
);


--
-- Name: agronomical_object_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agronomical_object
    ADD CONSTRAINT agronomical_object_pkey PRIMARY KEY (uri);


--
-- Name: at_project_project_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_project_project
    ADD CONSTRAINT at_project_project_pkey PRIMARY KEY (uri_project, uri_parent_project);


--
-- Name: at_project_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_project_users
    ADD CONSTRAINT at_project_users_pkey PRIMARY KEY (project_uri, users_email, type);


--
-- Name: at_trial_project_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_trial_project
    ADD CONSTRAINT at_trial_project_pkey PRIMARY KEY (project_uri, trial_uri);


--
-- Name: at_trial_users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_trial_users
    ADD CONSTRAINT at_trial_users_pkey PRIMARY KEY (trial_uri, users_email, type);


--
-- Name: pk_constraint; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public."group"
    ADD CONSTRAINT pk_constraint PRIMARY KEY (uri);


--
-- Name: pk_group_trial; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_group_trial
    ADD CONSTRAINT pk_group_trial PRIMARY KEY (group_uri, trial_uri);


--
-- Name: pk_group_users; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_group_users
    ADD CONSTRAINT pk_group_users PRIMARY KEY (group_uri, users_email);


--
-- Name: project_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_pkey PRIMARY KEY (uri);


--
-- Name: trial_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.trial
    ADD CONSTRAINT trial_pkey PRIMARY KEY (uri);


--
-- Name: users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (email);


--
-- Name: at_group_trial_group_uri_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_group_trial
    ADD CONSTRAINT at_group_trial_group_uri_fkey FOREIGN KEY (group_uri) REFERENCES public."group"(uri);


--
-- Name: at_group_trial_trial_uri_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_group_trial
    ADD CONSTRAINT at_group_trial_trial_uri_fkey FOREIGN KEY (trial_uri) REFERENCES public.trial(uri);


--
-- Name: at_group_users_group_uri_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_group_users
    ADD CONSTRAINT at_group_users_group_uri_fkey FOREIGN KEY (group_uri) REFERENCES public."group"(uri);


--
-- Name: at_group_users_users_email_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_group_users
    ADD CONSTRAINT at_group_users_users_email_fkey FOREIGN KEY (users_email) REFERENCES public.users(email);


--
-- Name: at_project_project_uri_parent_project_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_project_project
    ADD CONSTRAINT at_project_project_uri_parent_project_fkey FOREIGN KEY (uri_parent_project) REFERENCES public.project(uri);


--
-- Name: at_project_project_uri_project_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_project_project
    ADD CONSTRAINT at_project_project_uri_project_fkey FOREIGN KEY (uri_project) REFERENCES public.project(uri);


--
-- Name: at_project_users_users_email_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_project_users
    ADD CONSTRAINT at_project_users_users_email_fkey FOREIGN KEY (users_email) REFERENCES public.users(email);


--
-- Name: at_trial_project_trial_uri_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_trial_project
    ADD CONSTRAINT at_trial_project_trial_uri_fkey FOREIGN KEY (trial_uri) REFERENCES public.trial(uri);


--
-- Name: at_trial_users_trial_uri_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_trial_users
    ADD CONSTRAINT at_trial_users_trial_uri_fkey FOREIGN KEY (trial_uri) REFERENCES public.trial(uri);


--
-- Name: at_trial_users_users_email_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.at_trial_users
    ADD CONSTRAINT at_trial_users_users_email_fkey FOREIGN KEY (users_email) REFERENCES public.users(email);


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: -
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--
