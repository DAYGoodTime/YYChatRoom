package com.yychat.client.udp.handler;

import com.yychat.client.ClientMain;
import com.yychat.client.service.GroupService;
import com.yychat.client.view.MainWindow;
import com.yychat.common.model.Group;
import com.yychat.common.model.GroupMember;
import com.yychat.common.model.Message;
import com.yychat.common.model.ServiceResponse;

import javax.swing.*;
import java.util.List;

/**
 * 客户端群组消息处理器
 * 负责处理服务器端群组响应消息并更新UI
 */
public class GroupServiceHandler {

    private static final GroupService groupService = GroupService.getInstance();

    // ================================
    // 群组管理响应处理
    // ================================

    /**
     * 处理创建群组响应
     */
    public static void handleCreateGroupResponse(Message message) {
        try {
            MainWindow mainWindow = ClientMain.getMainWindow();
            if (mainWindow == null) {
                return;
            }

            if (!message.isJsonMessage()) {
                JOptionPane.showMessageDialog(mainWindow, "创建群组响应格式错误", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (message.getJson().getBool("success", false)) {
                Group group = message.getJson().getBean("data", Group.class);
                if (group != null) {
                    JOptionPane.showMessageDialog(mainWindow, "群组创建成功！群组ID: " + group.getGroupId(), "成功", JOptionPane.INFORMATION_MESSAGE);

                    // 更新群组列表显示
                    updateGroupListDisplay();

                    // 如果有群组面板，选中群组选项卡
                    if (mainWindow.getGroupListPanel() != null) {
                        // 后续可以添加切换到群组面板的逻辑
                    }
                } else {
                    JOptionPane.showMessageDialog(mainWindow, "创建群组数据异常", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                String errorMessage = message.getJson().getStr("message", "创建群组失败");
                JOptionPane.showMessageDialog(mainWindow, errorMessage, "创建群组失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("处理创建群组响应异常: " + e.getMessage());
        }
    }

    /**
     * 处理加入群组响应
     */
    public static void handleJoinGroupResponse(Message message) {
        try {
            MainWindow mainWindow = ClientMain.getMainWindow();
            if (mainWindow == null) {
                return;
            }

            if (!message.isJsonMessage()) {
                JOptionPane.showMessageDialog(mainWindow, "加入群组响应格式错误", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (message.getJson().getBool("success", false)) {
                Group group = message.getJson().getBean("data", Group.class);
                if (group != null) {
                    JOptionPane.showMessageDialog(mainWindow, "成功加入群组: " + group.getGroupName(), "成功", JOptionPane.INFORMATION_MESSAGE);

                    // 更新群组列表显示
                    updateGroupListDisplay();
                } else {
                    JOptionPane.showMessageDialog(mainWindow, "加入群组数据异常", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                String errorMessage = message.getJson().getStr("message", "加入群组失败");
                JOptionPane.showMessageDialog(mainWindow, errorMessage, "加入群组失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("处理加入群组响应异常: " + e.getMessage());
        }
    }

    /**
     * 处理退出群组响应
     */
    public static void handleLeaveGroupResponse(Message message) {
        try {
            MainWindow mainWindow = ClientMain.getMainWindow();
            if (mainWindow == null) {
                return;
            }

            if (!message.isJsonMessage()) {
                JOptionPane.showMessageDialog(mainWindow, "退出群组响应格式错误", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (message.getJson().getBool("success", false)) {
                String resultMessage = message.getJson().getStr("data", "成功退出群组");
                JOptionPane.showMessageDialog(mainWindow, resultMessage, "成功", JOptionPane.INFORMATION_MESSAGE);

                // 更新群组列表显示
                updateGroupListDisplay();
            } else {
                String errorMessage = message.getJson().getStr("message", "退出群组失败");
                JOptionPane.showMessageDialog(mainWindow, errorMessage, "退出群组失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("处理退出群组响应异常: " + e.getMessage());
        }
    }

    /**
     * 处理更新群组信息响应
     */
    public static void handleUpdateGroupInfoResponse(Message message) {
        try {
            MainWindow mainWindow = ClientMain.getMainWindow();
            if (mainWindow == null) {
                return;
            }

            if (!message.isJsonMessage()) {
                JOptionPane.showMessageDialog(mainWindow, "更新群组信息响应格式错误", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (message.getJson().getBool("success", false)) {
                Group group = message.getJson().getBean("data", Group.class);
                if (group != null) {
                    JOptionPane.showMessageDialog(mainWindow, "群组信息更新成功！", "成功", JOptionPane.INFORMATION_MESSAGE);

                    // 更新群组列表显示
                    updateGroupListDisplay();
                } else {
                    JOptionPane.showMessageDialog(mainWindow, "更新群组信息数据异常", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                String errorMessage = message.getJson().getStr("message", "更新群组信息失败");
                JOptionPane.showMessageDialog(mainWindow, errorMessage, "更新群组信息失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("处理更新群组信息响应异常: " + e.getMessage());
        }
    }

    /**
     * 处理删除群组响应
     */
    public static void handleDeleteGroupResponse(Message message) {
        try {
            MainWindow mainWindow = ClientMain.getMainWindow();
            if (mainWindow == null) {
                return;
            }

            if (!message.isJsonMessage()) {
                JOptionPane.showMessageDialog(mainWindow, "删除群组响应格式错误", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (message.getJson().getBool("success", false)) {
                String resultMessage = message.getJson().getStr("data", "成功删除群组");
                JOptionPane.showMessageDialog(mainWindow, resultMessage, "成功", JOptionPane.INFORMATION_MESSAGE);

                // 更新群组列表显示
                updateGroupListDisplay();
            } else {
                String errorMessage = message.getJson().getStr("message", "删除群组失败");
                JOptionPane.showMessageDialog(mainWindow, errorMessage, "删除群组失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("处理删除群组响应异常: " + e.getMessage());
        }
    }

    /**
     * 处理添加成员响应
     */
    public static void handleAddMemberResponse(Message message) {
        try {
            MainWindow mainWindow = ClientMain.getMainWindow();
            if (mainWindow == null) {
                return;
            }

            if (!message.isJsonMessage()) {
                JOptionPane.showMessageDialog(mainWindow, "添加成员响应格式错误", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (message.getJson().getBool("success", false)) {
                String resultMessage = message.getJson().getStr("data", "成功添加成员");
                JOptionPane.showMessageDialog(mainWindow, resultMessage, "成功", JOptionPane.INFORMATION_MESSAGE);

                // 更新群组成员列表显示（如果有群组管理窗口）
                updateGroupMembersDisplay();
            } else {
                String errorMessage = message.getJson().getStr("message", "添加成员失败");
                JOptionPane.showMessageDialog(mainWindow, errorMessage, "添加成员失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("处理添加成员响应异常: " + e.getMessage());
        }
    }

    /**
     * 处理移除成员响应
     */
    public static void handleRemoveMemberResponse(Message message) {
        try {
            MainWindow mainWindow = ClientMain.getMainWindow();
            if (mainWindow == null) {
                return;
            }

            if (!message.isJsonMessage()) {
                JOptionPane.showMessageDialog(mainWindow, "移除成员响应格式错误", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (message.getJson().getBool("success", false)) {
                String resultMessage = message.getJson().getStr("data", "成功移除成员");
                JOptionPane.showMessageDialog(mainWindow, resultMessage, "成功", JOptionPane.INFORMATION_MESSAGE);

                // 更新群组成员列表显示
                updateGroupMembersDisplay();
            } else {
                String errorMessage = message.getJson().getStr("message", "移除成员失败");
                JOptionPane.showMessageDialog(mainWindow, errorMessage, "移除成员失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("处理移除成员响应异常: " + e.getMessage());
        }
    }

    /**
     * 处理转让群主响应
     */
    public static void handleTransferOwnerResponse(Message message) {
        try {
            MainWindow mainWindow = ClientMain.getMainWindow();
            if (mainWindow == null) {
                return;
            }

            if (!message.isJsonMessage()) {
                JOptionPane.showMessageDialog(mainWindow, "转让群主响应格式错误", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (message.getJson().getBool("success", false)) {
                String resultMessage = message.getJson().getStr("data", "成功转让群主身份");
                JOptionPane.showMessageDialog(mainWindow, resultMessage, "成功", JOptionPane.INFORMATION_MESSAGE);

                // 更新群组成员列表显示
                updateGroupMembersDisplay();
            } else {
                String errorMessage = message.getJson().getStr("message", "转让群主身份失败");
                JOptionPane.showMessageDialog(mainWindow, errorMessage, "转让群主身份失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("处理转让群主响应异常: " + e.getMessage());
        }
    }

    // ================================
    // 查询响应处理
    // ================================

    /**
     * 处理群组成员列表响应
     */
    public static void handleGroupMembersResponse(Message message) {
//        try {
//            MainWindow mainWindow = ClientMain.getMainWindow();
//            if (mainWindow == null) {
//                return;
//            }
//
//            if (!message.isJsonMessage()) {
//                System.out.println("群组成员列表响应格式错误");
//                return;
//            }
//
//            if (message.getJson().getBool("success", false)) {
//                List<GroupMember> members = message.getJson().getBeanList("data", GroupMember.class);
//                if (members != null && !members.isEmpty()) {
//                    System.out.println("收到群组成员列表: " + members.size() + " 个成员");
//                    // 这里可以更新群组成员显示面板
//
//                    // 如果有群组管理窗口，更新成员列表
//                    if (mainWindow.getGroupMembersPanel() != null) {
//                        // 后续添加群组管理窗口时实现
//                    }
//                }
//            } else {
//                String errorMessage = message.getJson().getStr("message", "获取群组成员列表失败");
//                System.out.println("获取群组成员列表失败: " + errorMessage);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("处理群组成员列表响应异常: " + e.getMessage());
//        }
    }

    /**
     * 处理用户群组列表响应
     */
    public static void handleUserGroupsResponse(Message message) {
        try {
            MainWindow mainWindow = ClientMain.getMainWindow();
            if (mainWindow == null) {
                return;
            }

            if (!message.isJsonMessage()) {
                System.out.println("用户群组列表响应格式错误");
                return;
            }

            if (message.getJson().getBool("success", false)) {
                List<Group> groups = message.getJson().getBeanList("data", Group.class);
                if (groups != null) {
                    System.out.println("收到用户群组列表: " + groups.size() + " 个群组");

                    // 在新线程中更新UI，避免阻塞接收线程
                    new Thread(() -> {
                        if (mainWindow.getGroupListPanel() != null) {
                            // 后续添加群组列表面板时实现
                            // mainWindow.getGroupListPanel().setGroupList(groups);
                        }
                    }).start();
                }
            } else {
                String errorMessage = message.getJson().getStr("message", "获取用户群组列表失败");
                System.out.println("获取用户群组列表失败: " + errorMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("处理用户群组列表响应异常: " + e.getMessage());
        }
    }

    /**
     * 处理搜索群组响应
     */
    public static void handleGroupSearchResponse(Message message) {
        try {
            MainWindow mainWindow = ClientMain.getMainWindow();
            if (mainWindow == null) {
                return;
            }

            if (!message.isJsonMessage()) {
                System.out.println("搜索群组响应格式错误");
                return;
            }

            if (message.getJson().getBool("success", false)) {
                List<Group> groups = message.getJson().getBeanList("data", Group.class);
                if (groups != null) {
                    System.out.println("收到搜索群组结果: " + groups.size() + " 个群组");

                    // 在新线程中更新UI，避免阻塞接收线程
                    new Thread(() -> {
                        if (mainWindow.getGroupListPanel() != null) {
                            // 后续添加搜索结果显示时实现
                            // mainWindow.getGroupListPanel().updateSearchResults(groups);
                        }
                    }).start();
                }
            } else {
                String errorMessage = message.getJson().getStr("message", "搜索群组失败");
                System.out.println("搜索群组失败: " + errorMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("处理搜索群组响应异常: " + e.getMessage());
        }
    }

    /**
     * 处理群组聊天消息
     */
    public static void handleGroupChatMessage(Message message) {
//        try {
//            String sender = message.getSender();
//            String content = message.getContent();
//            System.out.println("收到群组消息 from " + sender + ": " + content);
//
//            MainWindow mainWindow = ClientMain.getMainWindow();
//            if (mainWindow != null) {
//                // 在新线程中处理UI更新
//                new Thread(() -> {
//                    // 这里可以打开或更新群组聊天窗口
//                    if (mainWindow.getGroupChatWindow() != null) {
//                        // 后续添加群组聊天窗口时实现
//                        // mainWindow.getGroupChatWindow().addMessage(sender, content, message.getTime());
//                    }
//                }).start();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("处理群组聊天消息异常: " + e.getMessage());
//        }
    }

    /**
     * 处理群组消息确认响应
     */
    public static void handleGroupChatMessageResponse(Message message) {
        try {
            if (message.getContent() != null && message.getContent().equals("消息已发送")) {
                System.out.println("群组消息发送确认: " + message.getContent());
                // 这里可以更新消息状态显示
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("处理群组消息确认响应异常: " + e.getMessage());
        }
    }

    /**
     * 更新群组列表显示
     */
    private static void updateGroupListDisplay() {
        MainWindow mainWindow = ClientMain.getMainWindow();
        if (mainWindow != null) {
            // 刷新用户群组列表
            ServiceResponse<List<Group>> response = groupService.getUserGroups();
            if (response.isSuccess() && response.getData() != null) {
                // 异步更新UI
                SwingUtilities.invokeLater(() -> {
                    if (mainWindow.getGroupListPanel() != null) {
                        // 后续添加群组列表面板时实现
                        // mainWindow.getGroupListPanel().setGroupList(response.getData());
                    }
                });
            }
        }
    }

    /**
     * 更新群组成员显示
     */
    private static void updateGroupMembersDisplay() {
        MainWindow mainWindow = ClientMain.getMainWindow();
        if (mainWindow != null) {
            // 如果有当前打开的群组，刷新成员列表
            // 这里需要获取当前选中的群组ID
            // groupService.getGroupMembers(currentGroupId).thenAccept(response -> {
            //     if (response.isSuccess()) {
            //         // 更新成员列表显示
            //     }
            // });
        }
    }

    /**
     * 显示错误消息
     */
    private static void showErrorMessage(String errorMessage) {
        MainWindow mainWindow = ClientMain.getMainWindow();
        if (mainWindow != null) {
            JOptionPane.showMessageDialog(mainWindow, errorMessage, "错误", JOptionPane.ERROR_MESSAGE);
        } else {
            System.err.println(errorMessage);
        }
    }
}