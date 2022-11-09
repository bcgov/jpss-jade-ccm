package ccm.models.system.justin;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ccm.models.system.justin.JustinAgencyFile;
import ccm.models.system.justin.JustinAccused;
import ccm.models.system.justin.JustinCourtFile;
import ccm.models.common.CommonCaseAccused;
import ccm.models.common.CommonChargeAssessmentCaseData;
import ccm.models.system.dems.DemsChargeAssessmentCaseData;
import ccm.models.system.dems.DemsPersonData;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JustinAgencyFileTest {

    @Test
    public void testHelloWorld() {
        assertEquals("hello world", "hello world");
    }

    //@Test
    public void testSampleJustinFiles() {

        String fileName = "json/justin/agency_file/justin_agency_file1.json";
        System.out.println(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()+"json/justin/agency_file/justin_agency_file1.json");

        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            JustinAgencyFile agencyFile = objectMapper.readValue(reader, JustinAgencyFile.class);
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

            StringWriter stringFile = new StringWriter();
            StringWriter stringFile2 = new StringWriter();
            StringWriter stringFile3 = new StringWriter();

            objectMapper.writeValue(stringFile, agencyFile);
            System.out.println("JustinAgencyFile JSON is\n"+stringFile);

            CommonChargeAssessmentCaseData businessFile = new CommonChargeAssessmentCaseData(agencyFile);
            objectMapper.writeValue(stringFile2, businessFile);

            System.out.println("\n\nCommonChargeAssessmentCaseData JSON is\n"+stringFile2);
            DemsChargeAssessmentCaseData demsCaseFile = new DemsChargeAssessmentCaseData(businessFile);

            objectMapper.writeValue(stringFile3, demsCaseFile);
            System.out.println("\n\nDemsChargeAssessmentCaseData JSON is\n"+stringFile3);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //@Test
    public void testTraverseJsonFiles() {
        String testFileFolder = getClass().getProtectionDomain().getCodeSource().getLocation().getPath()+"json/justin/agency_file";
        System.out.println(testFileFolder);
        File testJsonFolder = new File(testFileFolder);

        if(testJsonFolder.isDirectory()) {  // Run with JAR file
            System.out.println("this is a directory.");
            File [] files = testJsonFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("justin");
                }
            });

            for (File justinFile : files) {
                String justinFilePath = justinFile.getAbsolutePath();
                justinFile.getAbsolutePath();
                System.out.println("Justin File:" + justinFilePath);

                StringBuilder b = new StringBuilder(justinFilePath);
                b.replace(justinFilePath.lastIndexOf("justin"), justinFilePath.lastIndexOf("justin") + 6, "common" );
                String commonFilePath = b.toString();
                System.out.println("Common File:" + commonFilePath);

                b = new StringBuilder(justinFilePath);
                b.replace(justinFilePath.lastIndexOf("justin"), justinFilePath.lastIndexOf("justin") + 6, "dems" );
                String demsFilePath = b.toString();
                System.out.println("Dems File:" + demsFilePath);

                try {
                    byte[] jsonData = Files.readAllBytes(Paths.get(justinFilePath));
                    //create ObjectMapper instance
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

                    JustinAgencyFile agencyFile = objectMapper.readValue(jsonData, JustinAgencyFile.class);
                    CommonChargeAssessmentCaseData businessFile = new CommonChargeAssessmentCaseData(agencyFile);
                    DemsChargeAssessmentCaseData demsCaseFile = new DemsChargeAssessmentCaseData(businessFile);

                    StringWriter stringFile = new StringWriter();
                    objectMapper.writeValue(stringFile, agencyFile);
                    System.out.println("JustinAgencyFile JSON is\n"+stringFile);

                    StringWriter stringFile2 = new StringWriter();
                    objectMapper.writeValue(stringFile2, businessFile);
                    System.out.println("\n\nCommonChargeAssessmentCaseData JSON is\n"+stringFile2);

                    StringWriter stringFile3 = new StringWriter();
                    objectMapper.writeValue(stringFile3, demsCaseFile);
                    System.out.println("\n\nDemsChargeAssessmentCaseData JSON is\n"+stringFile3);

                } catch (IOException e) {
                    e.printStackTrace();
                }
        
    


            }
        } else {
            System.out.println("this is a something else.");
            // Run with IDE
            /*
            final URL url = Launcher.class.getResource("/" + path);
            if (url != null) {
                try {
                    final File apps = new File(url.toURI());
                    for (File app : apps.listFiles()) {
                        System.out.println(app);
                    }
                } catch (URISyntaxException ex) {
                    // never happens
                }
            }*/
        }
/*
        String fileName = "./json/justin/agency_file";
        final File folder = new File(fileName);
        for (final File fileEntry : folder.listFiles()) {
            System.out.println(fileEntry.getName());
        }*/
/*
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            JustinAgencyFile agencyFile = objectMapper.readValue(reader, JustinAgencyFile.class);
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

            StringWriter stringFile = new StringWriter();
            StringWriter stringFile2 = new StringWriter();
            StringWriter stringFile3 = new StringWriter();

            objectMapper.writeValue(stringFile, agencyFile);
            System.out.println("JustinAgencyFile JSON is\n"+stringFile);

            CommonChargeAssessmentCaseData businessFile = new CommonChargeAssessmentCaseData(agencyFile);
            objectMapper.writeValue(stringFile2, businessFile);

            System.out.println("\n\nCommonChargeAssessmentCaseData JSON is\n"+stringFile2);
            DemsChargeAssessmentCaseData demsCaseFile = new DemsChargeAssessmentCaseData(businessFile);

            objectMapper.writeValue(stringFile3, demsCaseFile);
            System.out.println("\n\nDemsChargeAssessmentCaseData JSON is\n"+stringFile3);

        } catch (IOException e) {
            e.printStackTrace();
        }
*/
    }

   //@Test
   public void testAgencyFileName() {
    String testFileFolder = getClass().getProtectionDomain().getCodeSource().getLocation().getPath()+"json/justin/agency_file";
    System.out.println(testFileFolder);
    File testJsonFolder = new File(testFileFolder);

    if(testJsonFolder.isDirectory()) {  // Run with JAR file
        System.out.println("this is a directory.");
        File [] files = testJsonFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("justin");
            }
        });

        for (File justinFile : files) {
            String justinFilePath = justinFile.getAbsolutePath();
            justinFile.getAbsolutePath();
            System.out.println("Justin File:" + justinFilePath);

            StringBuilder b = new StringBuilder(justinFilePath);
            b.replace(justinFilePath.lastIndexOf("justin"), justinFilePath.lastIndexOf("justin") + 6, "common" );
            String commonFilePath = b.toString();
            System.out.println("Common File:" + commonFilePath);

            b = new StringBuilder(justinFilePath);
            b.replace(justinFilePath.lastIndexOf("justin"), justinFilePath.lastIndexOf("justin") + 6, "dems" );
            String demsFilePath = b.toString();
            System.out.println("Dems File:" + demsFilePath);

            try {
                byte[] jsonData = Files.readAllBytes(Paths.get(justinFilePath));
                //create ObjectMapper instance
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

                JustinAgencyFile agencyFile = objectMapper.readValue(jsonData, JustinAgencyFile.class);
                CommonChargeAssessmentCaseData businessFile = new CommonChargeAssessmentCaseData(agencyFile);
                DemsChargeAssessmentCaseData demsCaseFile = new DemsChargeAssessmentCaseData(businessFile);

                StringWriter stringFile = new StringWriter();
                objectMapper.writeValue(stringFile, agencyFile);
                System.out.println("JustinAgencyFile JSON is\n"+stringFile);

                StringWriter stringFile2 = new StringWriter();
                objectMapper.writeValue(stringFile2, businessFile);
                System.out.println("\n\nCommonChargeAssessmentCaseData JSON is\n"+stringFile2);

                StringWriter stringFile3 = new StringWriter();
                objectMapper.writeValue(stringFile3, demsCaseFile);
                System.out.println("\n\nDemsChargeAssessmentCaseData JSON is\n"+stringFile3);

            } catch (IOException e) {
                e.printStackTrace();
            }
    



        }
    } else {
        System.out.println("this is a something else.");
        // Run with IDE
        /*
        final URL url = Launcher.class.getResource("/" + path);
        if (url != null) {
            try {
                final File apps = new File(url.toURI());
                for (File app : apps.listFiles()) {
                    System.out.println(app);
                }
            } catch (URISyntaxException ex) {
                // never happens
            }
        }*/
    }
/*
    String fileName = "./json/justin/agency_file";
    final File folder = new File(fileName);
    for (final File fileEntry : folder.listFiles()) {
        System.out.println(fileEntry.getName());
    }*/
/*
    ClassLoader classLoader = getClass().getClassLoader();

    try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
         InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
         BufferedReader reader = new BufferedReader(streamReader)) {

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        JustinAgencyFile agencyFile = objectMapper.readValue(reader, JustinAgencyFile.class);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        StringWriter stringFile = new StringWriter();
        StringWriter stringFile2 = new StringWriter();
        StringWriter stringFile3 = new StringWriter();

        objectMapper.writeValue(stringFile, agencyFile);
        System.out.println("JustinAgencyFile JSON is\n"+stringFile);

        CommonChargeAssessmentCaseData businessFile = new CommonChargeAssessmentCaseData(agencyFile);
        objectMapper.writeValue(stringFile2, businessFile);

        System.out.println("\n\nCommonChargeAssessmentCaseData JSON is\n"+stringFile2);
        DemsChargeAssessmentCaseData demsCaseFile = new DemsChargeAssessmentCaseData(businessFile);

        objectMapper.writeValue(stringFile3, demsCaseFile);
        System.out.println("\n\nDemsChargeAssessmentCaseData JSON is\n"+stringFile3);

    } catch (IOException e) {
        e.printStackTrace();
    }
*/
}

}