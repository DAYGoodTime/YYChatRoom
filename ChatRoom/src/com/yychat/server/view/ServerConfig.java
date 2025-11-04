package com.yychat.server.view;

public class ServerConfig {

    private int server_port_tcp = 3457;
    private int server_port_udp = 5678;
    private String datasrouce_url = "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf-8";
    private String datasource_username = "root";
    private String datasource_password = "kel123";

    public ServerConfig() {
    }

    public int getServer_port_tcp() {
        return server_port_tcp;
    }

    public void setServer_port_tcp(int server_port_tcp) {
        this.server_port_tcp = server_port_tcp;
    }

    public int getServer_port_udp() {
        return server_port_udp;
    }

    public void setServer_port_udp(int server_port_udp) {
        this.server_port_udp = server_port_udp;
    }

    public String getDatasrouce_url() {
        return datasrouce_url;
    }

    public void setDatasrouce_url(String datasrouce_url) {
        this.datasrouce_url = datasrouce_url;
    }

    public String getDatasource_username() {
        return datasource_username;
    }

    public void setDatasource_username(String datasource_username) {
        this.datasource_username = datasource_username;
    }

    public String getDatasource_password() {
        return datasource_password;
    }

    public void setDatasource_password(String datasource_password) {
        this.datasource_password = datasource_password;
    }
}
