package github.gpt.api.sync.db;

import github.gpt.api.sync.model.newapi.NewApiChannel;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DatabaseService {

    private static final String DB_URL;
    
    static {
        DB_URL = "jdbc:sqlite:" + github.gpt.api.sync.config.AppConfig.DATABASE_PATH;
        log.info("数据库路径: {}", DB_URL);
    }

    /**
     * 初始化数据库表结构
     */
    public void initDatabase() {
        String sql = """
            CREATE TABLE IF NOT EXISTS channels (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type INTEGER NOT NULL,
                key TEXT NOT NULL,
                status INTEGER NOT NULL DEFAULT 1,
                name TEXT NOT NULL UNIQUE,
                weight INTEGER DEFAULT 1,
                created_time INTEGER NOT NULL,
                test_time INTEGER,
                response_time INTEGER,
                base_url TEXT,
                other TEXT,
                balance REAL DEFAULT 0,
                balance_updated_time INTEGER,
                models TEXT,
                group_name TEXT,
                used_quota INTEGER DEFAULT 0,
                priority INTEGER DEFAULT 1,
                proxy TEXT
            );
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(sql);
            log.info("数据库表初始化成功");
            
            // 验证表结构
            verifyTableStructure(conn);
            
        } catch (SQLException e) {
            log.error("数据库初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    /**
     * 保存渠道列表到数据库
     * @param channels 渠道列表
     * @return 影响的行数
     */
    public int saveChannels(List<NewApiChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            log.warn("尝试保存空的渠道列表");
            return 0;
        }
        
        String upsertSql = """
            INSERT INTO channels (name, group_name, type, base_url, models, key, status, priority, created_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(name) DO UPDATE SET
                group_name = excluded.group_name,
                type = excluded.type,
                base_url = excluded.base_url,
                models = excluded.models,
                key = excluded.key,
                status = excluded.status,
                priority = excluded.priority;
        """;
        
        int affectedRows = 0;
        int successCount = 0;
        int errorCount = 0;
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(upsertSql)) {
            
            conn.setAutoCommit(false); // 开启事务
            
            for (NewApiChannel channel : channels) {
                try {
                    if (channel == null || channel.getName() == null || channel.getName().trim().isEmpty()) {
                        log.warn("跳过无效的渠道配置: {}", channel);
                        errorCount++;
                        continue;
                    }
                    
                    pstmt.setString(1, channel.getName());
                    pstmt.setString(2, channel.getGroupName());
                    pstmt.setInt(3, channel.getType());
                    pstmt.setString(4, channel.getBaseUrl());
                    pstmt.setString(5, channel.getModels());
                    pstmt.setString(6, channel.getKey());
                    pstmt.setInt(7, channel.getStatus());
                    pstmt.setLong(8, channel.getPriority() != null ? channel.getPriority() : 1);
                    pstmt.setLong(9, System.currentTimeMillis() / 1000);
                    pstmt.addBatch();
                    successCount++;
                    
                } catch (SQLException e) {
                    log.error("准备渠道数据时出错 - 渠道: {}, 错误: {}", channel.getName(), e.getMessage());
                    errorCount++;
                }
            }
            
            if (successCount > 0) {
                int[] result = pstmt.executeBatch();
                conn.commit(); // 提交事务
                
                for (int i : result) {
                    if (i > 0) {
                        affectedRows += i;
                    } else if (i == Statement.SUCCESS_NO_INFO || i == -2) {
                        // -2 表示ON CONFLICT DO UPDATE成功但返回特殊值
                        affectedRows++;
                    }
                }
            }
            
            log.info("数据库保存完成 - 成功: {}, 失败: {}, 影响行数: {}", successCount, errorCount, affectedRows);
            
        } catch (SQLException e) {
            log.error("保存渠道到数据库失败: {}", e.getMessage(), e);
            // 不抛出异常，返回已处理的数量
        }
        
        return affectedRows;
    }
    
    /**
     * 查询所有渠道
     * @return 渠道列表
     */
    public List<NewApiChannel> getAllChannels() {
        String sql = "SELECT * FROM channels ORDER BY created_time DESC";
        List<NewApiChannel> channels = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                NewApiChannel channel = new NewApiChannel();
                channel.setId(rs.getInt("id"));
                channel.setName(rs.getString("name"));
                channel.setGroupName(rs.getString("group_name"));
                channel.setType(rs.getInt("type"));
                channel.setBaseUrl(rs.getString("base_url"));
                channel.setModels(rs.getString("models"));
                channel.setKey(rs.getString("key"));
                channel.setStatus(rs.getInt("status"));
                channel.setPriority(rs.getLong("priority"));
                channels.add(channel);
            }
            
            log.info("查询到 {} 个渠道", channels.size());
            
        } catch (SQLException e) {
            log.error("查询渠道列表失败: {}", e.getMessage(), e);
        }
        
        return channels;
    }
    
    /**
     * 根据名称查询渠道
     * @param name 渠道名称
     * @return 渠道配置
     */
    public NewApiChannel getChannelByName(String name) {
        String sql = "SELECT * FROM channels WHERE name = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    NewApiChannel channel = new NewApiChannel();
                    channel.setId(rs.getInt("id"));
                    channel.setName(rs.getString("name"));
                    channel.setGroupName(rs.getString("group_name"));
                    channel.setType(rs.getInt("type"));
                    channel.setBaseUrl(rs.getString("base_url"));
                    channel.setModels(rs.getString("models"));
                    channel.setKey(rs.getString("key"));
                    channel.setStatus(rs.getInt("status"));
                    channel.setPriority(rs.getLong("priority"));
                    return channel;
                }
            }
            
        } catch (SQLException e) {
            log.error("查询渠道失败 - 名称: {}, 错误: {}", name, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 验证数据库表结构
     */
    private void verifyTableStructure(Connection conn) throws SQLException {
        String sql = "PRAGMA table_info(channels)";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int columnCount = 0;
            while (rs.next()) {
                columnCount++;
                String columnName = rs.getString("name");
                String columnType = rs.getString("type");
                log.debug("表字段: {} - 类型: {}", columnName, columnType);
            }
            
            log.info("channels表包含 {} 个字段", columnCount);
            
            if (columnCount == 0) {
                throw new SQLException("channels表不存在或没有字段");
            }
        }
    }
    
    /**
     * 测试数据库连接
     * @return 连接是否成功
     */
    public boolean testConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            return conn.isValid(5); // 5秒超时
        } catch (SQLException e) {
            log.error("数据库连接测试失败: {}", e.getMessage());
            return false;
        }
    }
}
