package com.redhat.lot;

import com.atlassian.oai.validator.model.Request;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.CharsetUtil;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class CustomRequest implements Request {

    private FullHttpRequest request;
    private Map<String, Collection<String>> headers = new HashMap<>();
    private List<String> qp = new ArrayList<>();
    private Map<String, String> qps = new HashMap<>();

    CustomRequest() {
    }

    CustomRequest(FullHttpRequest fullReq) {
        if (fullReq != null){
            this.request = fullReq;
            this.setupHeaders();
            this.setupQueryParameters();
        }
    }

    private void setupQueryParameters() {
        String uri = request.uri();
        if (request.uri() != null && !request.uri().equals("")) {
            if (uri.indexOf("?") > -1) {
                String queryPart = uri.substring(uri.indexOf("?"));
                if (queryPart.indexOf("&") > -1) {
                    String[] qps = queryPart.split("&");
                    for (String v : qps) {
                        qp.add(v);

                        String key = queryPart.substring(0, queryPart.indexOf("="));
                        String value = queryPart.substring(queryPart.indexOf("="));
                        this.qps.put(key, value);
                    }
                } else {
                    qp.add(queryPart);

                    String key = queryPart.substring(0, queryPart.indexOf("="));
                    String value = queryPart.substring(queryPart.indexOf("="));
                    this.qps.put(key, value);
                }
            }
        }

        System.out.println("Query Parameters List: ");
        for (String s : qp) {
            System.out.println(s);
        }

        System.out.println("Query Parameters hashmap: ");
        for (Map.Entry<String, String> entry : qps.entrySet()) {
            System.out.println("key: " + entry.getKey() + "value: " + entry.getValue());
        }
    }

    private void setupHeaders() {
        if (request != null && request.headers() != null){
            Iterator<Entry<String, String>> it = request.headers().iteratorAsString();
            while (it.hasNext()) {
                Entry<String, String> header = it.next();
                String[] values = header.getValue().split(";");
                List<String> listValues = new ArrayList<>();
                if (values != null) {
                    for (String v : values) {
                        listValues.add(v);
                    }
                } else {
                    listValues.add(header.getValue());
                }
                headers.put(header.getKey(), listValues);
            }
        }
    }

    @Override
    public String getPath() {
        String path = "";
        try{
            URL url = new URL(request.uri());
            path = url.getPath();
            System.out.println("PATH: "+path);
        }catch(java.net.MalformedURLException e){
            e.printStackTrace();
        }

        return path;
    }

    @Override
    public Method getMethod() {
        String name = request.method().name();
        Method method = Method.GET;
        if (name.equals(Method.GET.toString())) {
            method = Method.GET;
        } else if (name.equals(Method.POST.toString())) {
            method = Method.POST;
        } else if (name.equals(Method.DELETE.toString())) {
            method = Method.DELETE;
        } else if (name.equals(Method.OPTIONS.toString())) {
            method = Method.OPTIONS;
        } else if (name.equals(Method.PATCH.toString())) {
            method = Method.PATCH;
        } else if (name.equals(Method.PUT.toString())) {
            method = Method.PUT;
        } else if (name.equals(Method.TRACE.toString())) {
            method = Method.TRACE;
        }

        return method;
    }

    @Override
    public Optional<String> getBody() {
        return Optional.of(request.content().toString(CharsetUtil.UTF_8));
    }

    @Override
    public Collection<String> getQueryParameters() {
        return this.qp;
    }

    @Override
    public Collection<String> getQueryParameterValues(String name) {
        // temporary as is rare to have multiple parameters values
        List<String> list = new ArrayList<>();
        list.add(qps.get(name));
        return list;
    }

    @Override
    public Map<String, Collection<String>> getHeaders() {
        return headers;
    }

    @Override
    public Collection<String> getHeaderValues(String name) {
        return headers.get(name);
    }

}
