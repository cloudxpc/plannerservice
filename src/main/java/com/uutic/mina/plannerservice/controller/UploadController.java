package com.uutic.mina.plannerservice.controller;

import com.uutic.mina.plannerservice.entity.Plan;
import com.uutic.mina.plannerservice.entity.PlanItem;
import com.uutic.mina.plannerservice.repository.PlanItemRepository;
import com.uutic.mina.plannerservice.repository.PlanRepository;
import com.uutic.mina.plannerservice.util.JwtUtil;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

@RestController
@RequestMapping("/api/upload")
public class UploadController {
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyMMddHHmmss");

    @Value("${oss.url}")
    private String url;
    @Value("${oss.accessKeyId}")
    private String accessKeyId;
    @Value("${oss.accessKeySecret}")
    private String accessKeySecret;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private PlanItemRepository planItemRepository;

    private String postToOss(MultipartFile file) throws Exception {
        if (file.isEmpty())
            throw new Exception("file is empty");
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString() + extension;
        Map<String, String> formFields = createFormFields(accessKeyId, accessKeySecret, newFilename);
        String res = formUpload(url, formFields, newFilename, file.getInputStream());
//        byte[] bytes = file.getBytes();
//        Path path = Paths.get(uploadFolder, newFilename);
//        Files.write(path, bytes);
        return newFilename;
    }

    private Map<String, String> createFormFields(String accessKeyId, String accessKeySecret, String fileName) throws Exception {
        // 表单域
        Map<String, String> formFields = new LinkedHashMap<>();

        // key
        formFields.put("key", "${filename}");
        // Content-Disposition
        formFields.put("Content-Disposition", "attachment;filename=" + fileName);
        // OSSAccessKeyId
        formFields.put("OSSAccessKeyId", accessKeyId);
        // policy
        String policy = "{\"expiration\": \"2120-01-01T12:00:00.000Z\",\"conditions\": [[\"content-length-range\", 0, 104857600]]}";
        String encodePolicy = new String(Base64.encodeBase64(policy.getBytes()));
        formFields.put("policy", encodePolicy);
        // Signature
        String signaturecom = computeSignature(accessKeySecret, encodePolicy);
        formFields.put("Signature", signaturecom);

        return formFields;
    }

    private String computeSignature(String accessKeySecret, String encodePolicy) throws Exception {
        // convert to UTF-8
        byte[] key = accessKeySecret.getBytes("UTF-8");
        byte[] data = encodePolicy.getBytes("UTF-8");

        // hmac-sha1
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] sha = mac.doFinal(data);

        // base64
        return new String(Base64.encodeBase64(sha));
    }

    private String formUpload(String urlStr, Map<String, String> formFields, String fileName, InputStream inputStream)
            throws Exception {
        String res = "";
        HttpURLConnection conn = null;
        String boundary = "9431149156168";

        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN; rv:1.9.2.6)");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            OutputStream out = new DataOutputStream(conn.getOutputStream());

            // text
            if (formFields != null) {
                StringBuffer strBuf = new StringBuffer();
                Iterator<Entry<String, String>> iter = formFields.entrySet().iterator();
                int i = 0;

                while (iter.hasNext()) {
                    Entry<String, String> entry = iter.next();
                    String inputName = entry.getKey();
                    String inputValue = entry.getValue();

                    if (inputValue == null) {
                        continue;
                    }

                    if (i == 0) {
                        strBuf.append("--").append(boundary).append("\r\n");
                        strBuf.append("Content-Disposition: form-data; name=\""
                                + inputName + "\"\r\n\r\n");
                        strBuf.append(inputValue);
                    } else {
                        strBuf.append("\r\n").append("--").append(boundary).append("\r\n");
                        strBuf.append("Content-Disposition: form-data; name=\""
                                + inputName + "\"\r\n\r\n");
                        strBuf.append(inputValue);
                    }

                    i++;
                }
                out.write(strBuf.toString().getBytes());
            }

            // file
            String contentType = "application/octet-stream";
            StringBuffer strBuf = new StringBuffer();
            strBuf.append("\r\n").append("--").append(boundary)
                    .append("\r\n");
            strBuf.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n");
            strBuf.append("Content-Type: " + contentType + "\r\n\r\n");

            out.write(strBuf.toString().getBytes());

            int bytes = 0;
            byte[] bufferOut = new byte[1024];
            while ((bytes = inputStream.read(bufferOut)) != -1) {
                out.write(bufferOut, 0, bytes);
            }
            inputStream.close();

            byte[] endData = ("\r\n--" + boundary + "--\r\n").getBytes();
            out.write(endData);
            out.flush();
            out.close();

            // 读取返回数据
            strBuf = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                strBuf.append(line).append("\n");
            }
            res = strBuf.toString();
            reader.close();
            reader = null;
        } catch (Exception e) {
            System.err.println("Send post request exception: " + e);
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
        }

        return res;
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    @Transactional
    public String upload(@RequestParam("file") MultipartFile uploadFile, HttpServletRequest request) throws Exception {
        String userId = request.getAttribute("user_id").toString();
        String planId = request.getAttribute("plan_id").toString();
        String fileName = postToOss(uploadFile);

        PlanItem planItem = new PlanItem();
        planItem.setId(UUID.randomUUID().toString());
        planItem.setPlanId(planId);
        planItem.setOwner(userId);
        planItem.setResourceName(fileName);
        planItemRepository.save(planItem);

        return fileName;
    }

    @RequestMapping("/newplan")
    @Transactional
    public String newPlan() throws UnsupportedEncodingException {
        Plan plan = new Plan();
        plan.setId(UUID.randomUUID().toString());
        planRepository.save(plan);

        Map<String, String> claims = new HashMap<>();
        claims.put("plan_id", plan.getId());
        claims.put("user_id", "123");
        String token = JwtUtil.encode(claims);

        return token;
    }
}
