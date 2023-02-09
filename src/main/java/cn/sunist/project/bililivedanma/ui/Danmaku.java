package cn.sunist.project.bililivedanma.ui;

import cn.sunist.project.bililivedanma.model.GetRoomInfoRequest;
import cn.sunist.project.bililivedanma.model.GetRoomInfoResponse;
import cn.sunist.project.bililivedanma.service.Client;
import cn.sunist.project.bililivedanma.service.Server;
import com.google.gson.Gson;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang.StringUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Danmaku {
    // UI Components
    private JTabbedPane TabbedPanel;
    private JList<String> DanmaList;
    private JPanel Setting;
    private JTextField ListenPortInput;
    private JTextField ListenRoomInput;
    private JButton ConnectButton;
    private JTextField MiddlewareArgsInput;
    private JButton DisconnectButton;
    private JLabel StatusLabel;
    private JTextField MessageKeepCounter;
    private JPasswordField AccessTokenInput;
    private JTextField RefreshFrequencyInput;
    private JComboBox<String> MiddlewareModeComboBox;
    private JLabel RoomInfo;
    private JComboBox<String> TimeFormatComboBox;

    // ViewModels
    private final DefaultListModel<String> viewModel = new DefaultListModel<String>();
    private final DefaultComboBoxModel<String> modeChoices = new DefaultComboBoxModel<String>();
    private final DefaultComboBoxModel<String> timeFormatChoices = new DefaultComboBoxModel<String>();
    private Boolean refreshTaskInitialized = false;

    // Private Attributes
    private Server server;
    private Client client;

    private Integer listenPort;
    private Integer roomId;
    private String startMode;
    private String startArgs;
    private Integer messageKeepCount;
    private Integer refreshFrequency;
    private String accessToken;
    private Boolean readyToServe = false;
    private String backendUrl = "";
    private String timeFormat = "HH:mm:ss";

    /**
     * Return time.Now with formatted hh:mm:ss
     * @return formatted time string
     * @author sunist-c
     */
    private @NotNull String now() {
        Date date = new Date();
        SimpleDateFormat dateFormat= new SimpleDateFormat(timeFormat);
        return dateFormat.format(date);
    }

    /**
     * Parse a JTextField to Integer, when parsing a JTextField occurs an exception, returns null
     * @param element the JTextField wanted to parse
     * @return parsed result
     * @author sunist-c
     */
    private Integer parseJTextFieldToInteger(@NotNull JTextField element) {
        Integer result = null;
        String text = element.getText();
        try {
            result = Integer.parseInt(text);
        } catch (NumberFormatException err) {
            Messages.showMessageDialog("Error: Format element " + element.getName() + " error: \n"
                    + err.getMessage(),
                    "Error", null);
        }

        return result;
    }

    /**
     * Parse a JTextField to Integer, when parsing JTextField occurs an exception, returns defaultValue
     * @param element the JTextField wanted to parse
     * @param defaultValue the default value when parsing JTextField occurs an exception
     * @return parsed result
     * @author sunist-c
     */
    private @NotNull Integer parseJTextFieldToInteger(@NotNull JTextField element, @NotNull Integer defaultValue) {
        Integer result = defaultValue;
        String text = element.getText();
        try {
            result = Integer.parseInt(text);
        } catch (NumberFormatException err) {
            Messages.showMessageDialog("Error: Format element " + element.getName() + " error: \n"
                    + err.getMessage()
                    + "\nUse DefaultValue: " + defaultValue,
                    "Error", null);
        }

        return result;
    }

    /**
     * Initializes ui-components
     * @author sunist-c
     */
    private void initComponents() {
        ListenPortInput.setText("8086");
        ListenRoomInput.setText("");
        AccessTokenInput.setText("");

        modeChoices.removeAllElements();
        modeChoices.addElement("Manual");
//        modeChoices.addElement("Docker");
//        modeChoices.addElement("Backend");
        MiddlewareModeComboBox.setModel(modeChoices);

        timeFormatChoices.removeAllElements();
        timeFormatChoices.addElement("yyyy-MM-dd HH:mm:ss");
        timeFormatChoices.addElement("yyyy-MM-dd hh:mm:ss");
        timeFormatChoices.addElement("HH:mm:ss");
        timeFormatChoices.addElement("hh:mm:ss");
        timeFormatChoices.addElement("HH:mm");
        timeFormatChoices.addElement("hh:mm");
        TimeFormatComboBox.setModel(timeFormatChoices);

        MiddlewareArgsInput.setText("http://127.0.0.1:18080");
        MessageKeepCounter.setText("20");
        RefreshFrequencyInput.setText("1000");
    }

    /**
     * Load variables from ui-components
     * @author sunist-c
     */
    private void loadVariables() {
        listenPort = parseJTextFieldToInteger(ListenPortInput, 8086);
        roomId = parseJTextFieldToInteger(ListenRoomInput);
        startMode = (String) MiddlewareModeComboBox.getSelectedItem();
        startArgs = MiddlewareArgsInput.getText();
        messageKeepCount = parseJTextFieldToInteger(MessageKeepCounter, 15);
        accessToken = new String(AccessTokenInput.getPassword());
        refreshFrequency = parseJTextFieldToInteger(RefreshFrequencyInput, 3000);
        timeFormat = (String) TimeFormatComboBox.getSelectedItem();

        if (StringUtils.isBlank(startArgs)) {
            readyToServe = false;
            Messages.showMessageDialog("Error: Bad middleware args", "Error", null);
            return;
        }

        readyToServe = true;
    }

    /**
     * Initializes backend or manual arguments, this method will execute the backend program
     * @return start-up result, true means start backend successfully
     * @author sunist-c
     */
    private boolean initBackend() {
        // Docker Mode
        if ("Docker".equals(startMode)) {
            String[] command = new String[2];
            command[0] = "docker";
            command[1] = "run --rm --name=idea-danmaku-backend -p 18080:8080 " + startArgs;
            backendUrl = "http://127.0.0.1:18080";
            try {
                Runtime.getRuntime().exec(command);
            } catch (Exception err) {
                Messages.showMessageDialog("Error: Executing docker command\n"
                                + command[0] + " " + command[1]
                                + "\nfailed: \n"
                                + err.getMessage(),
                        "Error", null);
                reset();
                return false;
            }
        }

        // Backend Mode
        else if ("Backend".equals(startMode)) {
            File file = new File(startArgs);
            if (!file.exists()) {
                Messages.showMessageDialog("Error: No such file or directory: \n"
                                + startArgs,
                        "Error", null);
                reset();
                return false;
            }

            String[] command = new String[2];
            command[0] = startArgs;
            command[1] = "-b=true -p=18080";
            backendUrl = "http://127.0.0.1:18080";
            try {
                Runtime.getRuntime().exec(command);
            } catch (Exception err) {
                Messages.showMessageDialog("Error: Executing backend command\n"
                                + command[0] + " " + command[1]
                                + "\nfailed: \n"
                                + err.getMessage(),
                        "Error", null);
                reset();
                return false;
            }
        }

        // Manual Mode
        else if ("Manual".equals(startMode)) {
            backendUrl = startArgs;
        }

        // Unknown
        else {
            Messages.showMessageDialog("Unknown middleware mode",
                    "Error", null);
            reset();
            return false;
        }

        return true;
    }

    /**
     * Initializes the plugin http server
     * @return server status, true means successfully initialized http server
     * @author sunist-c
     */
    private boolean startServer() {
        server = new Server(listenPort, this);
        return server.Start();
    }

    /**
     * Initializes the http client
     * @author sunist-c
     */
    private void startClient() {
        client = new Client();
    }

    /**
     * Get room information
     * @return operation status, true means success
     * @author sunist-c
     */
    private boolean getRoomInfo() {
        try {
            GetRoomInfoRequest request = new GetRoomInfoRequest();

            request.baseRouter = "http://localhost:" + listenPort;
            request.room_id = roomId;
            request.danmakuRouter = "dm";
            request.giftRouter = "gift";
            request.guardRouter = "welcome";
            request.masterRouter = "custom_message";
            request.audienceRouter = "welcome";
            request.fansRouter = "custom_message";
            request.customMessage = "custom_message";
            request.exitRouter = "exit_callback";

            String jsonString = client.DoPostRequest(backendUrl + "/register", request, accessToken);
            if (StringUtils.isBlank(jsonString)) {
                System.out.println("Failed to register");
                return false;
            } else {
                System.out.println(jsonString);
                Gson gson = new Gson();
                GetRoomInfoResponse response = gson.fromJson(jsonString, GetRoomInfoResponse.class);
                UpdateRoomInfo(
                        response.room_id,
                        response.up_uid,
                        response.title,
                        response.tags,
                        response.live_status,
                        response.online
                );
                System.out.println("Updated");
                return true;
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            return false;
        }
    }

    /**
     * Connect to live room with backend
     * @return connection status, true means connected
     */
    private boolean connectToRoom() {
        // check connection status
        if (server != null) {
            locked();
            System.out.println("already connected");
            return true;
        }

        // load variables from ui-components
        loadVariables();
        if (!readyToServe) {
            Messages.showMessageDialog("Error: Cannot connect to live room with bad arguments", "Error", null);
            reset();
            return false;
        }

        // initialize backend
        if (!initBackend()) {
            System.out.println("Failed to initialize backend");
            return false;
        }

        // initialize http server
        if (!startServer()) {
            System.out.println("Failed to start server");
            return false;
        }

        // initialize http client
        startClient();

        // get room info
        if (!getRoomInfo()) {
            System.out.println("Failed to get room info");
            return false;
        }

        // setRefreshTask
        setRefreshTask();

        // update ui-components
        if (!server.Started()) {
            return false;
        } else {
            locked();
            return true;
        }
    }

    private void disconnectToRoom() {
        CloseableHttpClient httpClient = client.getHttpClient();
        HttpDelete deleteRequest = new HttpDelete(backendUrl + "/channel/" + roomId);
        deleteRequest.addHeader("Accept", "application/json");
        deleteRequest.addHeader("Content-Type", "application/json");
        deleteRequest.addHeader("Authorization", "Bearer " + accessToken);

        try {
            CloseableHttpResponse response = httpClient.execute(deleteRequest);
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != 200) {
                System.out.println("Bad response code: " + status.getStatusCode());
                return;
            }

            InputStream is = response.getEntity().getContent();
            is.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void exitRoom() {
        reset();
        Messages.showMessageDialog("Exited room: " + roomId + " with middleware notification", "Information", null);
    }

    private void setRefreshTask() {
        if (refreshTaskInitialized) {
            return;
        }

        TimerTask task = new TimerTask() {
            public void run() {
                DanmaList.setModel(viewModel);
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, refreshFrequency);
        refreshTaskInitialized = true;
    }

    public Danmaku() {
        initComponents();
        ConnectButton.addActionListener(e -> {
            if (connectToRoom()) {
                Messages.showMessageDialog("Successfully connected to room: " + roomId, "Information", null);
            } else {
                server = null;
                Messages.showMessageDialog("Failed to connect to room: " + roomId, "Error", null);
            }
        });
        DisconnectButton.addActionListener(e -> {
            disconnectToRoom();
            reset();
        });
    }

    public JComponent getComponent() {
        return TabbedPanel;
    }

    public void SendDanmaMessage(String userName, String message) {
        String labelText = "<html><body>"
                + now() + " - " + userName + " said: " + "<br>"
                + message
                + "</body></html>";
        sendMessage(labelText);
    }

    public void SendGiftMessage(String userName, String giftName, String action, Integer price, Integer number) {
        String labelText = "<html><body>"
                + now() + " - " + userName + " " + action + ": " + "<br>"
                + giftName + "*" + number + ": $" + price
                + "</body></html>";
        sendMessage(labelText);
    }

    public void UpdateRoomInfo(Integer roomId, Integer upUid, String title, String tags, Boolean liveStatus, Integer audiences) {
        String textMessage = "<html><body>"
                + "<h1>" + "RoomInfo" + "</h1>"
                + "RoomID: " + roomId + "<br>"
                + "UP: " + upUid + "<br>"
                + "OnLive: " + liveStatus + "<br>"
                + "Audiences: " + audiences + "<br>"
                + "Title: " + title + "<br>"
                + "Tags: " + tags
                + "</body></html>";
        this.RoomInfo.setText(textMessage);
    }

    public void SendCustomMessage(String textMessage) {
        String labelText = "<html><body>"
                + now() + " - " + "system.info :" + "<br>"
                + textMessage
                + "</body></html>";
        sendMessage(labelText);
    }

    public void SendAudienceCome(String audienceName) {
        String labelText = "<html><body>"
                + now() + " - system.welcome: " + "<br>"
                + "welcome new audience: " + audienceName + " come to our live room!"
                + "</body></html>";
        sendMessage(labelText);
    }

    public void sendMessage(String message) {
        viewModel.addElement(message);
        if (viewModel.size() >= messageKeepCount) {
            viewModel.remove(0);
        }
    }

    public void reset() {
        if (server != null) {
            server.Close();
        }
        server = null;
        StatusLabel.setText("Ready");
        ConnectButton.setEnabled(true);
        ListenRoomInput.setEnabled(true);
        ListenPortInput.setEnabled(true);
        MiddlewareArgsInput.setEnabled(true);
        AccessTokenInput.setEnabled(true);
        MessageKeepCounter.setEnabled(true);
        MiddlewareModeComboBox.setEnabled(true);
        TimeFormatComboBox.setEnabled(true);
        viewModel.removeAllElements();
    }

    public void locked() {
        StatusLabel.setText("Serving...");
        ConnectButton.setEnabled(false);
        ListenRoomInput.setEnabled(false);
        ListenPortInput.setEnabled(false);
        MiddlewareArgsInput.setEnabled(false);
        AccessTokenInput.setEnabled(false);
        MessageKeepCounter.setEnabled(false);
        MiddlewareModeComboBox.setEnabled(false);
        RefreshFrequencyInput.setEnabled(false);
        TimeFormatComboBox.setEnabled(false);
    }

    public void error(String errorMessage) {
        disconnectToRoom();
        reset();
        StatusLabel.setText("Error: " + errorMessage);
    }
}
