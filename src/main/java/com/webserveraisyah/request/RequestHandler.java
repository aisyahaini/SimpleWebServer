package com.webserveraisyah.request;

import java.io.*; // kelas-kelas yang terkait dengan operasi input/output
import java.net.HttpURLConnection; // untuk koneksi HTTP
import java.net.InetAddress; // ntuk representasi alamat IP
import java.text.SimpleDateFormat; // SimpleDateFormat untuk pemformatan tanggal
import java.util.Date; // untuk representasi tanggal dan waktu
import java.util.Properties; // untuk bekerja dengan file konfigurasi
import java.io.File; //untuk bekerja dengan file
import java.io.FileInputStream; // FileInputStream untuk membaca file
import java.io.IOException; // IOException untuk penanganan kesalahan input/output
import java.io.OutputStream; // OutputStream untuk menulis data keluar
import java.nio.file.Files; // untuk operasi file
import java.nio.file.Path; // untuk merepresentasikan lokasi file
import java.nio.file.Paths; // ntuk membuat objek Path

import javafx.application.Platform; //ntuk menjalankan tugas di utas JavaFX
import javafx.scene.control.TextArea; // Mengimpor kelas TextArea dari JavaFX untuk menampilkan teks

import com.sun.net.httpserver.HttpExchange; // HttpExchange untuk mewakili pertukaran HTTP
import com.sun.net.httpserver.HttpHandler; // HttpHandler untuk menangani permintaan HTTP


// Deklarasi kelas RequestHandler yang mengimplementasikan HttpHandler
public class RequestHandler implements HttpHandler {

    private String webDirectory; // Deklarasi variabel untuk direktori web
    private String logDirectory; // Deklarasi variabel untuk direktori log
    private static final String CONFIG_FILE = "D:\\AISYAH\\KULIAH\\SEMESTER 4\\PEMROGRAMAN BERBASIS OBJEK\\TUGAS\\UAS_WebServer_AisyahNuraini_14161\\config.properties"; // Deklarasi konstanta untuk lokasi file konfigurasi
    public TextArea logTextArea; // Deklarasi variabel untuk TextArea yang digunakan untuk menampilkan log

    // Konstruktor untuk kelas RequestHandler
    public RequestHandler(String webDirectory, String logDirectory, TextArea logTextArea) {
        this.webDirectory = webDirectory; // Menginisialisasi direktori web
        this.logDirectory = logDirectory; // Menginisialisasi direktori log
        this.logTextArea = logTextArea; // Menginisialisasi TextArea untuk log
    }

    @Override
    // Implementasi metode handle dari antarmuka HttpHandler
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath(); // Mendapatkan jalur permintaan dari URI permintaan

        // inetAddress => kelas dari paket java.net yang digunakan untuk mewakili alamat IP baik IPv4 maupun IPv6
        InetAddress clientAddress = exchange.getRemoteAddress().getAddress(); // Mendapatkan alamat IP klien
        String clientIpAddress = clientAddress.getHostAddress(); // Mendapatkan alamat IP klien

        logAccess(clientIpAddress, requestPath); // Memanggil metode logAccess untuk mencatat akses

        Path requestedFile = Paths.get(webDirectory, requestPath.substring(1)); // Mendapatkan Path file yang diminta
        if (Files.isDirectory(requestedFile)) { // Memeriksa apakah permintaan adalah sebuah direktori
            handleFolderRequest(exchange, requestedFile); // Memanggil metode handleFolderRequest untuk menangani permintaan direktori
        } else {
            handleFileRequest(exchange, requestedFile); // Memanggil metode handleFileRequest untuk menangani permintaan file
        }

        String requestLog = exchange.getRequestMethod() + " " + requestPath + " " + exchange.getProtocol(); // Membuat log permintaan
        log(requestLog); // Memanggil metode log untuk mencatat log
        Platform.runLater(() -> { // Menjalankan tugas di JavaFX
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy"); // Membuat objek SimpleDateFormat untuk format tanggal
            String timestamp = dateFormat.format(new Date()); // Mendapatkan timestamp
            if (!logTextArea.getText().contains(requestLog)) { // Memeriksa apakah log sudah ditampilkan sebelumnya
                logTextArea.appendText("[" + timestamp + "] Request From " + "/" + clientIpAddress + " "  + requestLog + "\n"); // Menambahkan log ke TextArea
            }
        });
    }

    // Metode untuk menangani permintaan folder
    // Metode untuk menangani permintaan folder
    private void handleFolderRequest(HttpExchange exchange, Path folderPath) throws IOException {
        File[] files = folderPath.toFile().listFiles(); // Mendapatkan daftar file dalam folder
        StringBuilder response = new StringBuilder(); // Membuat StringBuilder untuk membangun respon
        response.append("<html><body><h1>Index of ").append(folderPath.toString()).append("</h1>"); // Memulai respon HTML
        response.append("<table border=\"1\">"); // Membuat tabel dengan border
        response.append("<tr><th>Name</th><th>Type</th><th>Size (Bytes)</th></tr>"); // Menambahkan header tabel

        if (files != null) {
            for (File file : files) { // Loop melalui setiap file dalam folder
                String link = file.isDirectory() ? file.getName() + "/" : file.getName(); // Mendapatkan link untuk setiap file
                response.append("<tr>"); // Memulai baris baru
                response.append("<td><a href=\"").append(link).append("\">").append(file.getName()).append("</a></td>"); // Menambahkan kolom nama dengan tautan
                response.append("<td>").append(file.isDirectory() ? "Directory" : "File").append("</td>"); // Menambahkan kolom tipe
                response.append("<td>").append(file.isDirectory() ? "-" : file.length()).append("</td>"); // Menambahkan kolom ukuran
                response.append("</tr>"); // Menutup baris
            }
        }

        response.append("</table>"); // Menutup tabel
        response.append("</body></html>"); // Menutup respon HTML
        sendResponse(exchange, HttpURLConnection.HTTP_OK, response.toString()); // Mengirim respon ke klien
    }


    // Metode untuk menangani permintaan file
    private void handleFileRequest(HttpExchange exchange, Path filePath) throws IOException {
        if (Files.exists(filePath) && !Files.isDirectory(filePath)) { // Memeriksa apakah file ada dan bukan direktori
            byte[] fileBytes = Files.readAllBytes(filePath); // Membaca semua byte dari file
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, fileBytes.length); // Mengirim status OK ke klien
            OutputStream outputStream = exchange.getResponseBody(); // Mendapatkan aliran keluaran respons
            outputStream.write(fileBytes); // Menulis byte file ke aliran keluaran
            outputStream.close(); // Menutup aliran keluaran
        } else { // Jika file tidak ditemukan
            String response = "Not Found"; // Respon not found
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, response.getBytes().length); // Mengirim status not found ke klien
            OutputStream outputStream = exchange.getResponseBody(); // Mendapatkan aliran keluaran respons
            outputStream.write(response.getBytes()); // Menulis respon not found ke aliran keluaran
            outputStream.close(); // Menutup aliran keluaran
        }
    }

    // Metode untuk mencatat akses
    public void logAccess(String ipAddress, String requestPath) {
        String logFilePath = generateLogFilePath(); // Mendapatkan lokasi file log
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); // Objek SimpleDateFormat untuk format tanggal
        String dateString = dateFormat.format(new Date()); // Mendapatkan tanggal dalam format yang diinginkan
        String logEntry = "[" + new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").format(new Date()) + "] Request From " + "/" + ipAddress + " GET " + requestPath + " HTTP/1.1" + "\n"; // Membuat entri log
        appendToLogFile(logEntry, "server_" + dateString + ".log"); // Menambahkan entri log ke file log
        try {
            File logFile = new File(logFilePath); // Membuat objek File untuk file log
            if (!logFile.exists()) { // Memeriksa apakah file log sudah ada
                logFile.createNewFile(); // Jika belum, buat file log baru
            }
            Files.write(Paths.get(logFilePath), logEntry.getBytes(), java.nio.file.StandardOpenOption.APPEND); // Menulis entri log ke file log
        } catch (IOException e) { // Menangani kesalahan IOException
            e.printStackTrace(); // Mencetak jejak kesalahan
        }
    }

    // Metode untuk menghasilkan lokasi file log
    public String generateLogFilePath() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); // Objek SimpleDateFormat untuk format tanggal
        String dateString = dateFormat.format(new Date()); // Mendapatkan tanggal dalam format yang diinginkan
        return Paths.get(logDirectory, "server_" + dateString + ".log").toString(); // Mengembalikan lokasi file log
    }

    // Metode untuk mengirim respons ke klien
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length); // Mengirim status kode dan panjang respons
        OutputStream outputStream = exchange.getResponseBody(); // Mendapatkan aliran keluaran respons
        outputStream.write(response.getBytes()); // Menulis respons ke aliran keluaran
        outputStream.close(); // Menutup aliran keluaran
    }

    // Metode untuk mencatat log
    private void log(String message) {
        // Metode ini bisa diimplementasikan untuk mencatat pesan log jika diperlukan
        // diisi komentar karena method nya direncakan di masa depan
    }


    // Metode untuk menambahkan entri log ke file log
    public void appendToLogFile(String logMessage, String logFileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(logDirectory, logFileName).toString(), true))) { // Membuka file log untuk menambahkan entri
            writer.append(logMessage); // Menambahkan entri log ke file log
        } catch (IOException e) { // Menangani kesalahan IOException
            e.printStackTrace(); // Mencetak jejak kesalahan
        }
    }

    // Metode untuk menyimpan konfigurasi server ke dalam file properti
    public static void saveConfig(int port, String webDirectory, String logDirectory) {
        try {
            Properties prop = new Properties(); // Membuat objek Properties untuk menyimpan konfigurasi
            prop.setProperty("port", String.valueOf(port)); // Menetapkan port ke properti
            prop.setProperty("webDirectory", webDirectory); // Menetapkan direktori web ke properti
            prop.setProperty("logDirectory", logDirectory); // Menetapkan direktori log ke properti
            prop.store(new FileOutputStream(CONFIG_FILE), null); // Menyimpan properti ke file konfigurasi
        } catch (IOException e) { // Menangani kesalahan IOException
            e.printStackTrace(); // Mencetak jejak kesalahan
        }
    }

    /// Metode untuk mendapatkan nilai port dari file konfigurasi
    public static int getPort() {
        try {
            Properties prop = new Properties(); // Membuat objek Properties untuk membaca konfigurasi
            prop.load(new FileInputStream(CONFIG_FILE)); // Memuat konfigurasi dari file
            return Integer.parseInt(prop.getProperty("port")); // Mendapatkan nilai port dari properti
        } catch (IOException e) { // Menangani kesalahan IOException
            e.printStackTrace(); // Mencetak jejak kesalahan
            return 8080; // Default port
        }
    }

    // Metode untuk mendapatkan lokasi direktori web dari file konfigurasi
    public static String getWebDirectory() {
        try {
            Properties prop = new Properties(); // Membuat objek Properties untuk membaca konfigurasi
            prop.load(new FileInputStream(CONFIG_FILE)); // Memuat konfigurasi dari file
            return prop.getProperty("webDirectory"); // Mendapatkan lokasi direktori web dari properti
        } catch (IOException e) { // Menangani kesalahan IOException
            e.printStackTrace(); // Mencetak jejak kesalahan
            return ""; // Default web directory
        }
    }

    // Mendapatkan letak log server dari file konfigurasi config.properties
    public static String getLogDirectory() {
        try {
            Properties prop = new Properties(); // Membuat objek Properties untuk membaca konfigurasi
            prop.load(new FileInputStream(CONFIG_FILE)); // Memuat konfigurasi dari file
            return prop.getProperty("logDirectory"); // Mendapatkan lokasi direktori log dari properti
        } catch (IOException e) { // Menangani kesalahan IOException
            e.printStackTrace(); // Mencetak jejak kesalahan
            return ""; // Default log directory
        }
    }


}
