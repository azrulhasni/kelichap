/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.service;
   import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.stereotype.Service;
/**
 *
 * @author azrul
 */
@Service
public class TypesenseCollectionCloner {
 

    private static final String API_URL = "http://localhost:8108/collections?src_name=kelichap-users";
    private static final String API_KEY = "CsNJV1e8679TXrNGIpj6mfH0Kr04mbYQ";

    public static void main(String[] args) {
        try {
            // Create JSON payload
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("name", "new_coll");

            // Create URL object
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Set request method to POST
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Set headers
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-TYPESENSE-API-KEY", API_KEY);

            // Write payload to output stream
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Check response code
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Optional: Read response if needed
            // BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            // StringBuilder response = new StringBuilder();
            // String responseLine = null;
            // while ((responseLine = br.readLine()) != null) {
            //     response.append(responseLine.trim());
            // }
            // System.out.println(response.toString());

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
