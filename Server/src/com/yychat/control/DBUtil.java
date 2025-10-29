package com.yychat.control;

import com.yychat.model.User;
import com.yychat.tcp.model.TransferStatus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class DBUtil {
    private static final String db_url = "jdbc:mysql://localhost:3306/yychat2022s?useUnicode=true&characterEncoding=utf-8";
    private static final String db_user = "root";
    private static final String db_pass = "kel123";
    private static Connection dataBase;

    public static boolean connectDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            dataBase = DriverManager.getConnection(db_url, db_user, db_pass);
        } catch (Exception e) {
            System.out.println("数据库连接失败");
            return false;
        }
        return true;
    }

    public static boolean loginValidate(String userName,String password){
        boolean loginSuccess = false;
        String query = "select * from user where username=? and password=?";
        PreparedStatement statement = null;
        try{
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            statement.setString(2, password);
            ResultSet rs = statement.executeQuery();
            loginSuccess = rs.next();
        }catch (Exception e){
            e.printStackTrace();
        }
        return loginSuccess;
    }

    public static boolean hasUser(String user){
        boolean hasUser = false;
        String query = "select * from user where username=?";
        PreparedStatement statement = null;
        try{
            statement = dataBase.prepareStatement(query);
            statement.setString(1, user);
            ResultSet rs = statement.executeQuery();
            hasUser = rs.next();
        }catch (Exception e){
            e.printStackTrace();
        }
        return hasUser;
    }

    //添加新用户（支持头像路径）
    public static int addNewUser(User user){
        int result = -1;
        String insert = "insert into user(username,password,avatar_path) values(?,?,?)";
        PreparedStatement statement;
        try{
            statement = dataBase.prepareStatement(insert);
            statement.setString(1, user.getUserName());
            statement.setString(2, user.getPassword());
            // 使用用户对象中的avatarPath，如果没有则使用默认值
            String avatarPath = user.getAvatarPath();
            if (avatarPath == null || avatarPath.trim().isEmpty()) {
                avatarPath = "0.jpg";
            }
            statement.setString(3, avatarPath);
            result = statement.executeUpdate();
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public static List<String> getAllFriends(String userName,int friendType){
        List<String> friendList = new ArrayList<>();
        String query = "select slaveUser from userRelation where masterUser=? and relation=?";
        PreparedStatement statement = null;
        try{
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            statement.setInt(2, friendType);
            ResultSet rs = statement.executeQuery();
            while(rs.next()){
                friendList.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return friendList;
    }

    public static List<String> getUnknowUsers(String userName){
        List<String> result = new ArrayList<>();
        String query = "select u.username from user as u " +
                "left join userrelation as ur on u.username = ur.slaveuser and ur.masteruser =? " +
                "where ur.masteruser is null and u.username !=?";
        PreparedStatement statement = null;
        try{
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            statement.setString(2, userName);
            ResultSet rs = statement.executeQuery();
            while(rs.next()){
                result.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean isUsersFriend(String userName,String userFriend,int friendType){
        boolean result = false;
        String query = "select * from userRelation where masterUser=? and slaveUser=? and relation=?";
        PreparedStatement statement = null;
        try{
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            statement.setString(2, userFriend);
            statement.setInt(3, friendType);
            ResultSet rs = statement.executeQuery();
            result = rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static int insertIntoFriend(String user,String friendOfUser,int friendType){
        int count = 0;
        String query = "insert into userRelation(masterUser,slaveUser,relation) values(?,?,?)";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, user);
            statement.setString(2, friendOfUser);
            statement.setInt(3, friendType);
            count = statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public static boolean insertChatMessage(String from, String to, String content, LocalDateTime time){
        boolean result = false;
        String query="insert into message(sender,receiver,content,sendtime) values(?,?,?,?)";
        PreparedStatement statement=null;
        try{
            statement = dataBase.prepareStatement(query);
            statement.setString(1, from);
            statement.setString(2, to);
            statement.setString(3, content);
            statement.setTimestamp(4,new java.sql.Timestamp(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
            result = (statement.executeUpdate() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 更新用户头像路径
     * @param userName 用户名
     * @param avatarPath 头像路径
     * @return 更新是否成功
     */
    public static boolean updateUserAvatar(String userName, String avatarPath) {
        boolean result = false;
        String query = "UPDATE user SET avatar_path=? WHERE username=?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, avatarPath);
            statement.setString(2, userName);
            result = (statement.executeUpdate() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取用户头像路径
     * @param userName 用户名
     * @return 头像路径，如果用户不存在返回默认头像
     */
    public static String getUserAvatar(String userName) {
        String avatarPath = "0.jpg"; // 默认头像
        String query = "SELECT avatar_path FROM user WHERE username=?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                avatarPath = rs.getString("avatar_path");
                if (avatarPath == null || avatarPath.trim().isEmpty()) {
                    avatarPath = "0.jpg"; // 如果数据库中为空，返回默认头像
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return avatarPath;
    }

    /**
     * 获取用户完整信息（包括头像路径）
     * @param userName 用户名
     * @return 用户对象，如果用户不存在返回null
     */
    public static User getUserInfo(String userName) {
        User user = null;
        String query = "SELECT username, avatar_path FROM user WHERE username=?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                user = new User();
                user.setUserName(rs.getString("username"));
                String avatarPath = rs.getString("avatar_path");
                if (avatarPath != null && !avatarPath.trim().isEmpty()) {
                    user.setAvatarPath(avatarPath);
                } else {
                    user.setAvatarPath("0.jpg"); // 默认头像
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

    // ==================== 文件传输记录管理 ====================

    /**
     * 创建文件传输记录表
     */
    public static boolean createFileTransferTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS file_transfer_records (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "transfer_id VARCHAR(64) UNIQUE NOT NULL," +
                "sender VARCHAR(50) NOT NULL," +
                "receiver VARCHAR(50) NOT NULL," +
                "file_name VARCHAR(255) NOT NULL," +
                "file_size BIGINT NOT NULL," +
                "file_type VARCHAR(100)," +
                "transfer_method VARCHAR(10) NOT NULL," +
                "status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED', 'PAUSED', 'WAITING_CONFIRMATION', 'INTERRUPTED') DEFAULT 'PENDING'," +
                "progress_percent INT DEFAULT 0," +
                "file_hash VARCHAR(64)," +
                "transfer_speed BIGINT DEFAULT 0," +
                "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "end_time TIMESTAMP NULL," +
                "error_message TEXT," +
                "chunk_index INT DEFAULT 0," +
                "total_chunks INT DEFAULT 1," +
                "INDEX idx_transfer_id (transfer_id)," +
                "INDEX idx_sender (sender)," +
                "INDEX idx_receiver (receiver)," +
                "INDEX idx_status (status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        Statement statement = null;
        try {
            statement = dataBase.createStatement();
            statement.execute(createTableSQL);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 保存文件传输记录
     */
    public static boolean saveFileTransferRecord(String transferId, String sender, String receiver,
                                               String fileName, long fileSize, String fileType,
                                               String transferMethod, TransferStatus status) {
        String insertSQL = "INSERT INTO file_transfer_records (transfer_id, sender, receiver, file_name, " +
                "file_size, file_type, transfer_method, status, start_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(insertSQL);
            statement.setString(1, transferId);
            statement.setString(2, sender);
            statement.setString(3, receiver);
            statement.setString(4, fileName);
            statement.setLong(5, fileSize);
            statement.setString(6, fileType);
            statement.setString(7, transferMethod);
            statement.setString(8, status.name());
            statement.setTimestamp(9, new java.sql.Timestamp(System.currentTimeMillis()));

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取传输状态
     */
    public static TransferStatus getTransferStatus(String transferId) {
        String querySQL = "SELECT status FROM file_transfer_records WHERE transfer_id = ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(querySQL);
            statement.setString(1, transferId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                String statusStr = rs.getString("status");
                return TransferStatus.valueOf(statusStr);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 更新传输进度
     */
    public static boolean updateTransferProgress(String transferId, int progressPercent) {
        return updateTransferProgress(transferId, progressPercent, 0, null);
    }

    /**
     * 更新传输进度（带速度信息）
     */
    public static boolean updateTransferProgress(String transferId, int progressPercent, long transferSpeed, String errorMessage) {
        String updateSQL = "UPDATE file_transfer_records SET progress_percent = ?, transfer_speed = ?, " +
                "end_time = CASE WHEN ? >= 100 THEN CURRENT_TIMESTAMP ELSE end_time END " +
                "WHERE transfer_id = ?";

        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(updateSQL);
            statement.setInt(1, progressPercent);
            statement.setLong(2, transferSpeed);
            statement.setInt(3, progressPercent);
            statement.setString(4, transferId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 更新传输状态
     */
    public static boolean updateTransferStatus(String transferId, TransferStatus status) {
        return updateTransferStatus(transferId, status, null);
    }

    /**
     * 更新传输状态（带错误信息）
     */
    public static boolean updateTransferStatus(String transferId, TransferStatus status, String errorMessage) {
        String updateSQL = "UPDATE file_transfer_records SET status = ?, end_time = CURRENT_TIMESTAMP " +
                "WHERE transfer_id = ?";

        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(updateSQL);
            statement.setString(1, status.name());
            statement.setString(2, transferId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 更新分片传输信息
     */
    public static boolean updateChunkTransfer(String transferId, int chunkIndex, int totalChunks) {
        String updateSQL = "UPDATE file_transfer_records SET chunk_index = ?, total_chunks = ? " +
                "WHERE transfer_id = ?";

        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(updateSQL);
            statement.setInt(1, chunkIndex);
            statement.setInt(2, totalChunks);
            statement.setString(3, transferId);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取用户的传输记录
     */
    public static List<String> getUserTransferHistory(String userName, int limit) {
        List<String> transferHistory = new ArrayList<>();
        String querySQL = "SELECT transfer_id, sender, receiver, file_name, file_size, transfer_method, status, " +
                "progress_percent, start_time, end_time FROM file_transfer_records " +
                "WHERE sender = ? OR receiver = ? " +
                "ORDER BY start_time DESC LIMIT ?";

        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(querySQL);
            statement.setString(1, userName);
            statement.setString(2, userName);
            statement.setInt(3, limit);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                StringBuilder record = new StringBuilder();
                record.append("ID: ").append(rs.getString("transfer_id")).append(" | ");
                record.append("文件: ").append(rs.getString("file_name")).append(" | ");
                record.append("大小: ").append(formatFileSize(rs.getLong("file_size"))).append(" | ");
                record.append("方式: ").append(rs.getString("transfer_method")).append(" | ");
                record.append("状态: ").append(rs.getString("status")).append(" | ");
                record.append("进度: ").append(rs.getInt("progress_percent")).append("%");

                transferHistory.add(record.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return transferHistory;
    }

    /**
     * 获取传输统计信息
     */
    public static TransferStatistics getTransferStatistics(String userName) {
        TransferStatistics stats = new TransferStatistics();
        String querySQL = "SELECT " +
                "COUNT(*) as total_transfers, " +
                "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_transfers, " +
                "SUM(CASE WHEN transfer_method = 'TCP' THEN 1 ELSE 0 END) as tcp_transfers, " +
                "SUM(CASE WHEN transfer_method = 'UDP' THEN 1 ELSE 0 END) as udp_transfers, " +
                "SUM(file_size) as total_bytes, " +
                "AVG(progress_percent) as avg_progress " +
                "FROM file_transfer_records " +
                "WHERE sender = ? OR receiver = ?";

        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(querySQL);
            statement.setString(1, userName);
            statement.setString(2, userName);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                stats.totalTransfers = rs.getInt("total_transfers");
                stats.completedTransfers = rs.getInt("completed_transfers");
                stats.tcpTransfers = rs.getInt("tcp_transfers");
                stats.udpTransfers = rs.getInt("udp_transfers");
                stats.totalBytes = rs.getLong("total_bytes");
                stats.averageProgress = rs.getDouble("avg_progress");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return stats;
    }

    /**
     * 清理过期的传输记录
     */
    public static int cleanExpiredRecords(int daysOld) {
        String deleteSQL = "DELETE FROM file_transfer_records WHERE start_time < DATE_SUB(NOW(), INTERVAL ? DAY)";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(deleteSQL);
            statement.setInt(1, daysOld);
            return statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取活跃传输列表
     */
    public static List<String> getActiveTransfers() {
        List<String> activeTransfers = new ArrayList<>();
        String querySQL = "SELECT transfer_id, sender, receiver, file_name, status, progress_percent " +
                "FROM file_transfer_records " +
                "WHERE status IN ('PENDING', 'IN_PROGRESS', 'WAITING_CONFIRMATION') " +
                "ORDER BY start_time DESC";

        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(querySQL);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                StringBuilder transfer = new StringBuilder();
                transfer.append(rs.getString("transfer_id")).append(" - ");
                transfer.append(rs.getString("sender")).append(" → ");
                transfer.append(rs.getString("receiver")).append(" - ");
                transfer.append(rs.getString("file_name")).append(" (");
                transfer.append(rs.getString("status")).append(", ");
                transfer.append(rs.getInt("progress_percent")).append("%)");

                activeTransfers.add(transfer.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return activeTransfers;
    }

    // ==================== 工具方法 ====================

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 传输统计信息内部类
     */
    public static class TransferStatistics {
        public int totalTransfers = 0;
        public int completedTransfers = 0;
        public int tcpTransfers = 0;
        public int udpTransfers = 0;
        public long totalBytes = 0;
        public double averageProgress = 0.0;

        public double getSuccessRate() {
            return totalTransfers > 0 ? (double) completedTransfers / totalTransfers * 100 : 0;
        }

        public double getTCPRate() {
            return totalTransfers > 0 ? (double) tcpTransfers / totalTransfers * 100 : 0;
        }

        public String toString() {
            return String.format("总传输: %d, 完成: %d, 成功率: %.1f%%, TCP比例: %.1f%%, 总大小: %s",
                    totalTransfers, completedTransfers, getSuccessRate(), getTCPRate(), formatFileSize(totalBytes));
        }
    }

    public static void main(String[] args) {
        //TESTING DRIVER
        boolean b = connectDB();
        if(b){
            System.out.println("Success");
            // 测试文件传输记录表创建
            if (createFileTransferTable()) {
                System.out.println("文件传输记录表创建成功");
            } else {
                System.out.println("文件传输记录表创建失败");
            }
        }else
            System.out.println("Fail");

        System.out.println(getUnknowUsers("day"));
    }
}
