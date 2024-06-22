/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.service;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author azrul
 */
@Service
public class ConversionService {
    private static final String[] extensions = {".bib", ".doc", ".xml", ".docx", ".fodt", ".html", ".ltx", ".txt", ".odt", ".ott", ".pdb", ".pdf", ".psw", ".rtf", ".sdw", ".stw", ".sxw", ".uot", ".vor", ".wps", ".epub", ".png", ".bmp", ".emf", ".eps", ".fodg", ".gif", ".jpg", ".met", ".odd", ".otg", ".pbm", ".pct", ".pgm", ".ppm", ".ras", ".std", ".svg", ".svm", ".swf", ".sxd", ".sxw", ".tiff", ".xhtml", ".xpm", ".fodp", ".potm", ".pot", ".pptx", ".pps", ".ppt", ".pwp", ".sda", ".sdd", ".sti", ".sxi", ".uop", ".wmf", ".csv", ".dbf", ".dif", ".fods", ".ods", ".ots", ".pxl", ".sdc", ".slk", ".stc", ".sxc", ".uos", ".xls", ".xlt", ".xlsx", ".tif", ".jpeg", ".odp"};
    private final String endpoint;
    private final MultipartEntityBuilder builder;
    private final CloseableHttpClient client;
    private static final String LIBRE_OFFICE_ROUTE = "forms/libreoffice/convert";

    public ConversionService(@Value("${gotenberg.url}") String endpoint) throws MalformedURLException {
        if (!isValidURL(endpoint)) {
            throw new MalformedURLException();
        }
        this.endpoint = endpoint.endsWith("/")?endpoint:endpoint+"/";
        this.builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.EXTENDED);
        this.client = HttpClients.createDefault();
    }
    
    public String[] getConvertibleFileExt(){
        return extensions;
    }

    public byte[] convertToPDF(byte[] file, String fileName) throws IOException {
        

        if (!StringUtils.endsWithAny(fileName, extensions)) {
            throw new FileNotFoundException("File extensions are not supported");
        }

        this.builder.addBinaryBody(fileName, file,ContentType.DEFAULT_BINARY, fileName);

        HttpPost httpPost = new HttpPost(endpoint.concat(LIBRE_OFFICE_ROUTE));
        HttpEntity requestEntity = this.builder.build();
        httpPost.setEntity(requestEntity);
        return this.client.execute(httpPost,response->{
            byte[] converted =  IOUtils.toByteArray(response.getEntity().getContent());
            return converted;
        });
    }
    
     public static boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
}
