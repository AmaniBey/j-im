package org.dromara.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.exception.base.BaseException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.im.domain.GroupChat;
import org.dromara.im.domain.GroupUserChat;
import org.dromara.im.domain.SimpleUser;
import org.dromara.im.domain.bo.GroupChatBo;
import org.dromara.im.domain.bo.GroupUserChatBo;
import org.dromara.im.domain.vo.GroupChatVo;
import org.dromara.im.mapper.GroupChatMapper;
import org.dromara.im.mapper.GroupUserChatMapper;
import org.dromara.im.service.IGroupChatService;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysOssMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.dromara.im.utils.SysUserUtils.*;

/**
 * 群聊Service业务层处理
 *
 * @author luojiaju
 * @date 2024-01-11
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class GroupChatServiceImpl implements IGroupChatService {


    private final GroupChatMapper baseMapper;

    private final SysUserMapper sysUserMapper;

    private final SysOssMapper sysOssMapper;

    private final GroupUserChatMapper groupUserChatMapper;

    /**
     * 查询群聊详细信息
     *
     * @param id 群聊id
     */
    @Override
    public Map<String, Object> queryById(String id) {
        CompletableFuture<Integer> groupCountFuture =
            CompletableFuture.supplyAsync(() -> baseMapper.selectGroupCountByGroupId(id))
                .exceptionally(ex -> {
                    log.error("查询群聊数量异常:{}", ex.getMessage());
                    throw new ServiceException(ex.getMessage());
                });

        CompletableFuture<List<SimpleUser>> userIdsFuture = CompletableFuture.supplyAsync(() -> {
            List<Long> userIds = baseMapper.selectGroupChatUsersByGroupId(id);
            List<SysUser> sysUsers = sysUserMapper.selectBatchIds(userIds);

            return MapstructUtils.convert(sysUsers, SimpleUser.class);
        }).exceptionally(ex -> {
            log.error("查询群聊用户异常:{}", ex.getMessage());
            throw new ServiceException(ex.getMessage());
        });

        CompletableFuture<GroupChatVo> groupChatVoFuture =
            CompletableFuture.supplyAsync(() -> baseMapper.selectVoById(id))
                .exceptionally(ex -> {
                    log.error("查询群聊详细信息异常:{}", ex.getMessage());
                    throw new ServiceException(ex.getMessage());
                });
        return groupCountFuture.thenCombine(userIdsFuture, (groupCount, userIds) -> {
            Map<String, Object> info = new HashMap<>();
            info.put("groupUserCount", groupCount);
            info.put("userList", userIds);
            return info;
        }).thenCombine(groupChatVoFuture, (info, groupChatVo) -> {
            info.put("groupChat", groupChatVo);
            return info;
        }).join();
    }

    /**
     * 查询群聊列表
     */
    @Override
    public TableDataInfo<GroupChatVo> queryPageList(GroupChatBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<GroupChat> lqw = buildQueryWrapper(bo);
        Page<GroupChatVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        List<String> avatarIds = result.getRecords().stream().map(GroupChatVo::getAvatar).toList();
        List<SysOssVo> ossVoList = sysOssMapper.selectVoBatchIds(avatarIds);
        Map<Long, String> ossMap = ossVoList.stream().collect(Collectors.toMap(SysOssVo::getOssId, SysOssVo::getUrl));
        result.getRecords().forEach(item -> {
            if (item.getAvatar() != null) {
                item.setAvatar(ossMap.get(item.getAvatar()));
            }
        });


        return TableDataInfo.build(result);
    }

    private LambdaQueryWrapper<GroupChat> buildQueryWrapper(GroupChatBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<GroupChat> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getId() != null, GroupChat::getId, bo.getId())
            .like(bo.getGroupName() != null, GroupChat::getGroupName, bo.getGroupName())
            .between(bo.getCreateTime() != null, GroupChat::getCreateTime, params.get("StartTime"), params.get("entTime"));
        return lqw;
    }

    /**
     * 查询用户的群聊列表
     * userId
     * groupName
     */
    @Override
    public List<GroupChatVo> queryList(GroupChatBo bo) {
        return baseMapper.selectAssociationByGroupIdAndUserId(bo.getParams());
    }

    /**
     * 新增群聊
     */
    @Transactional
    @Override
    public GroupChatBo insertByBo(GroupChatBo bo) {
        // 添加自己加入群聊
        String currUserId = getCurrentUserIdToString();
        bo.getUserIds().add(currUserId);

        GroupChat add = MapstructUtils.convert(bo, GroupChat.class);
        // 设置群主
        add.setGroupLeaderId(currUserId);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
            // 设置关联表
            List<GroupUserChat> groupUserChats = bo.getUserIds().stream().map(userId -> {
                GroupUserChat groupUserChat = new GroupUserChat();
                groupUserChat.setGroupId(add.getId());
                groupUserChat.setUserId(userId);
                groupUserChat.setRole(bo.getRole());
                return groupUserChat;
            }).toList();
            groupUserChatMapper.insertBatch(groupUserChats);
            return bo;
        }
        throw new ServiceException("创建群聊失败,请联系管理员!!!");
    }

    public Boolean userJoiningGroupChat(GroupChatBo bo) {
        if (bo.getUserIds().isEmpty()) {
            return false;
        }
        String id = bo.getId();
        List<String> userIds = bo.getUserIds();
        return baseMapper.insertAssociationUserJoiningGroupChat(id, userIds);

    }

    /**
     * 修改群聊
     */
    @Override
    public Boolean updateByBo(GroupChatBo bo) {
        GroupChat update = MapstructUtils.convert(bo, GroupChat.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(GroupChat entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除群聊
     */
    @Transactional
    @Override
    public Boolean deleteWithValidByGroupIdAndUserId(String groupId, String userId) {
        boolean b = baseMapper.deleteById(groupId) > 0;
        if (b) {
            LambdaQueryWrapper<GroupUserChat> queryWrapper = Wrappers.lambdaQuery();
            queryWrapper.eq(GroupUserChat::getGroupId, groupId);
            return groupUserChatMapper.delete(queryWrapper) > 0;
        }
        return b;
    }


    /**
     * 查询当前用户的群聊列表
     */
    @Override
    public TableDataInfo<GroupChatVo> queryTheCurrentUsersGroupChatList(GroupChatBo bo, PageQuery pageQuery) {
        QueryWrapper<GroupChat> query = Wrappers.query();
        query.eq("gu.user_id ", getCurrentUserId());
        query.like(StringUtils.isNotEmpty(bo.getGroupName()), "ig.group_name", bo.getGroupName())
            .orderBy(true, true, "gu.top")
            .orderByDesc("gu.create_time ")
            .orderByDesc("ig.create_time");
        Page<GroupChat> page = pageQuery.build();
        return TableDataInfo.build(groupUserChatMapper.selectUserGroup(query, page));
    }

    /**
     * 通过群聊id,查询用户列表
     */
    @Override
    public TableDataInfo<SimpleUser> queryUserListThroughGroupChatId(GroupChatBo bo, PageQuery pageQuery) {
        QueryWrapper<GroupChat> query = Wrappers.query();
        query.eq("gu.group_id ", bo.getId())
            .orderByDesc("su.user_id");
        Page<GroupChat> page = pageQuery.build();
        Page<SysUserVo> result = groupUserChatMapper.selectUserList(query, page);
        List<SimpleUser> onlineUsers = getOnlineUsers(result.getRecords());
        return TableDataInfo.build(onlineUsers);
    }

    /**
     * 退出群聊
     *
     * @return
     */
    @Override
    public Boolean removeByGroupIdAndUserId(String groupId, String userId) {
        LambdaQueryWrapper<GroupUserChat> query = Wrappers.lambdaQuery();
        query.eq(GroupUserChat::getUserId, userId)
            .eq(GroupUserChat::getGroupId, groupId);
        return this.groupUserChatMapper.delete(query) > 0;
    }

    /**
     * 修改群聊角色
     */
    @Override
    public int editRoleByUserId(String userId, String groupId, Integer role) {
        if (userId.equals(groupId)) {
            throw new ServiceException("不能修改自己的角色");
        }
        // [1] 获得当前群聊信息
        GroupChatVo groupChatVo = baseMapper.selectVoById(groupId);
        if (groupChatVo != null) {
            // [2] 只有群主才能修改角色
            Serializable currentUserId = getCurrentUserId();
            if (groupChatVo.getGroupLeaderId().equals(currentUserId)) {
                LambdaUpdateWrapper<GroupUserChat> luw = Wrappers.lambdaUpdate();
                luw.set(GroupUserChat::getRole, role)
                    .eq(GroupUserChat::getUserId, userId)
                    .eq(GroupUserChat::getGroupId, groupId);
                // 修改角色
                return groupUserChatMapper.update(luw);
            } else {
                throw new ServiceException("您不是群主无法更改角色!!!");
            }
        }
        return 0;
    }


    /**
     * 置顶
     */
    @Override
    public int top(String groupId, Integer top) {
        Serializable currentUserId = getCurrentUserId();
        LambdaUpdateWrapper<GroupUserChat> luw = Wrappers.lambdaUpdate();
        luw.set(GroupUserChat::getTop, top)
            .eq(GroupUserChat::getUserId, currentUserId)
            .eq(GroupUserChat::getGroupId, groupId);
        return groupUserChatMapper.update(luw);
    }

    /**
     * 邀请用户加入群聊
     *
     * @param bo
     */
    @Override
    public Boolean insertByGroupUserChatBo(GroupUserChatBo bo) {
//        是否存在
        LambdaQueryWrapper<GroupUserChat> lqw = Wrappers.lambdaQuery();
        lqw.eq(GroupUserChat::getGroupId, bo.getGroupId()).and(lqw1 -> lqw1.eq(GroupUserChat::getUserId, bo.getUserId()));
        if (groupUserChatMapper.selectCount(lqw) > 0) {
            throw new BaseException("400", "用户已存在该私聊内");
        }
        GroupUserChat chat = MapstructUtils.convert(bo, GroupUserChat.class);
        if (groupUserChatMapper.insert(chat) > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 更新最近消息
     */
    public Boolean updateRecentMessage(LambdaUpdateWrapper<GroupChat> luw) {
        return baseMapper.update(null, luw) > 0;
    }
}

