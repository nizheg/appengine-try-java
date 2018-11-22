/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package myapp;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DemoServlet extends HttpServlet {

    /**
     * Serialization UID.
     */
    private static final long serialVersionUID = 1L;
    private List<String> allowedResponseHeaders = Arrays.asList("Content-Type");
    private List<String> restrictedRequestHeaders = Arrays.asList("cookie", "host", "x-proxy-url");
    private List<String> restrictedCookies = Arrays.asList("JSESSIONID", "Domain");

    public void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws
            IOException, ServletException {
        String checkHeader = httpServletRequest.getHeader("X-Proxy-Url");
        if (checkHeader == null) {
            httpServletResponse.getOutputStream().print("X-Proxy-Url is not defined");
            return;
        }
        Connection connection = createConnection(checkHeader, httpServletRequest);
        connection.method(Connection.Method.GET);
        Connection.Response response = connection.execute();
        fillResponse(response, httpServletResponse);
    }

    public void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws
            IOException,
            ServletException {
        String checkHeader = httpServletRequest.getHeader("X-Proxy-Url");
        if (checkHeader == null) {
            httpServletResponse.getOutputStream().print("X-Proxy-Url is not defined");
            return;
        }
        Connection connection = createConnection(checkHeader, httpServletRequest);
        connection.method(Connection.Method.POST);
        Connection.Response response = connection.execute();
        fillResponse(response, httpServletResponse);
    }

    private void fillResponse(Connection.Response response, HttpServletResponse httpServletResponse) throws
            IOException {
        for (Map.Entry<String, String> cookiePair : response.cookies().entrySet()) {
            if (!restrictedCookies.contains(cookiePair.getKey())) {
                httpServletResponse.addCookie(new Cookie(cookiePair.getKey(), cookiePair.getValue()));
            }
        }
        for (Map.Entry<String, String> headerPair : response.headers().entrySet()) {
            if (allowedResponseHeaders.contains(headerPair.getKey())) {
                httpServletResponse.addHeader(headerPair.getKey(), headerPair.getValue());
            }
        }
        httpServletResponse.addHeader("X-Location", response.url().toString());
        httpServletResponse.getOutputStream().write(response.bodyAsBytes());
        httpServletResponse.getOutputStream().flush();
    }

    private Connection createConnection(String url, HttpServletRequest httpServletRequest) {
        Connection connection = Jsoup.connect(url);
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (!restrictedCookies.contains(cookie.getName())) {
                    connection.cookie(cookie.getName(), cookie.getValue());
                }
            }
        }
        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!restrictedRequestHeaders.contains(headerName)) {
                    connection.header(headerName, httpServletRequest.getHeader(headerName));
                }
            }
        }
        Map<String, String[]> parameters = httpServletRequest.getParameterMap();
        if (parameters != null) {
            for (Map.Entry<String, String[]> data : parameters.entrySet()) {
                for (String value : data.getValue()) {
                    connection.data(data.getKey(), value);
                }
            }
        }
        connection.maxBodySize(10 * 1024 * 1024);
        return connection;
    }
}
