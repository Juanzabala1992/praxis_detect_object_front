import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import javax.xml.bind.DatatypeConverter;
import javax.faces.context.PartialViewContext;
import java.util.Map;

@ManagedBean
@ViewScoped
public class ImageProcessingBean {
    public UploadedFile uploadedFile;
    private byte[] detectedImage;
    String responseJsonF;

    public void handleFileUpload(FileUploadEvent event) {
        System.out.println("Inicio");
        uploadedFile = event.getFile();
        System.out.println("Inicio2 " + uploadedFile.getContents());
        this.responseJsonF = "";
    }

    public void processImage() {
        if (uploadedFile != null) {
            try {
                byte[] imageData = uploadedFile.getContents();
                System.out.println("upload file imageData " + uploadedFile.getContents());
                // Establecer la conexión con el backend
                URL url = new URL("http://127.0.0.1:5000/detect");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                // Enviar la imagen al backend
                connection.getOutputStream().write(imageData);

                // Leer la respuesta del backend
                InputStream inputStream = connection.getInputStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                int bytesRead;
                byte[] buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                byte[] responseBytes = outputStream.toByteArray();
                String responseJson = new String(responseBytes);

                System.out.println("out " + responseJson);
                this.responseJsonF = responseJson;
                this.processDetectionResult(responseJson);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void processDetectionResult(String responseJson) {
        System.out.println("detectionObjects " + responseJson);
        // Convertir el JSON en una lista de objetos de detección
        List<DetectionObject> detectionObjects = new Gson().fromJson(responseJson,
                new TypeToken<List<DetectionObject>>() {
                }.getType());
        System.out.println("detectionObjects 1" + detectionObjects);
        // Obtener la imagen original
        byte[] originalImageBytes = uploadedFile.getContents();
        System.out.println("detectionObjects " + detectionObjects);
        // Dibujar los cuadros de detección sobre la imagen
        detectedImage = drawRectanglesOnImage(originalImageBytes, detectionObjects);
        System.out.println("detectedImage " + detectedImage);
        this.getDetectedImageAsBase64();
        FacesContext context = FacesContext.getCurrentInstance();
        PartialViewContext partialViewContext = context.getPartialViewContext();
        partialViewContext.getRenderIds().add("imagePanel");
    }

    private byte[] drawRectanglesOnImage(byte[] imageBytes, List<DetectionObject> detectionObjects) {
        try {
            // Convertir los bytes de la imagen en un objeto BufferedImage
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

            // Obtener el objeto Graphics2D para dibujar sobre la imagen
            Graphics2D g2d = image.createGraphics();

            // Configurar el color y el grosor del borde del rectángulo
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(2));

            // Dibujar un rectángulo para cada objeto de detección
            for (DetectionObject detection : detectionObjects) {
                Position position = detection.getPosition();
                int x = position.getX();
                int y = position.getY();
                int width = position.getWidth();
                int height = position.getHeight();
                g2d.drawRect(x, y, width, height);
            }

            // Liberar los recursos del objeto Graphics2D
            g2d.dispose();

            // Convertir la imagen modificada de nuevo a bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            byte[] modifiedImageBytes = outputStream.toByteArray();
            System.out.println("modifiedImageBytes " + modifiedImageBytes);
            return modifiedImageBytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Clase para representar un objeto de detección
    public class DetectionObject {
        private String object;
        private Position position;

        // Constructor, getters y setters para DetectionObject
        public Position getPosition() {
            return position;
        }

        public void setPosition(Position position) {
            this.position = position;
        }
    }

    // Clase para representar la posición de un objeto de detección
    public class Position {
        private int x;
        private int y;
        private int width;
        private int height;

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    // Getters y setters
    public byte[] getDetectedImage() {
        return detectedImage;
    }

    public String getDetectedImageAsBase64() {
        if (detectedImage != null) {
            System.out.println(DatatypeConverter.printBase64Binary(detectedImage));
            return DatatypeConverter.printBase64Binary(detectedImage);
        } else {
            return null;
        }
    }

    public String getResponseJson() {
        if (responseJsonF != null) {
            // Convertir la cadena JSON a un array de objetos Java
            Gson gson = new Gson();
            Object[] objects = gson.fromJson(responseJsonF, Object[].class);

            // Construir una cadena formateada con los valores deseados
            StringBuilder formattedResponse = new StringBuilder();
            formattedResponse.append("Objeto(s): ");
            for (Object object : objects) {
                if (object instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) object;
                    if (map.containsKey("object")) {
                        formattedResponse.append(map.get("object")).append(", ");
                    }
                }
            }
            // Eliminar la última coma y espacio
            if (formattedResponse.length() > 0) {
                formattedResponse.delete(formattedResponse.length() - 2, formattedResponse.length());
            }
            return formattedResponse.toString();
        } else {
            return "";
        }
    }
}
