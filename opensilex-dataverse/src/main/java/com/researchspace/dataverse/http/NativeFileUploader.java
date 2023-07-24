package com.researchspace.dataverse.http;

import org.springframework.core.io.AbstractResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * NativeFileUploader is  helper class that performs upload of files using native API
 */
public class NativeFileUploader {

    public HttpEntity<MultiValueMap<String, Object>> createFileUploadEntity(FileUploadMetadata metadata, String apiKey, AbstractResource resource){

        MultiValueMap<String,Object> multipartRequest = new LinkedMultiValueMap<>();

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(AbstractOpsImplV1.apiHeader, apiKey);
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);//Main request's headers

        HttpHeaders requestHeadersAttachment = new HttpHeaders();

        HttpEntity attachmentPart = new HttpEntity<>(resource,requestHeadersAttachment);
        multipartRequest.set("file",attachmentPart);

        HttpHeaders requestHeadersJSON = new HttpHeaders();
        requestHeadersJSON.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<FileUploadMetadata> requestEntityJSON = new HttpEntity<>(metadata, requestHeadersJSON);
        multipartRequest.set("jsonData",requestEntityJSON);

        HttpEntity<MultiValueMap<String,Object>> requestEntity = new HttpEntity<>(multipartRequest,requestHeaders);//final request
        return requestEntity;

    }
}
