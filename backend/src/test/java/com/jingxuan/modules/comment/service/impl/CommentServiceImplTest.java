package com.jingxuan.modules.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.SysRole;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.Work;
import com.jingxuan.entity.WorkComment;
import com.jingxuan.entity.WorkMember;
import com.jingxuan.entity.WorkPublish;
import com.jingxuan.enums.RoleEnum;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysRoleMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkCommentMapper;
import com.jingxuan.mapper.WorkMapper;
import com.jingxuan.mapper.WorkMemberMapper;
import com.jingxuan.mapper.WorkPublishMapper;
import com.jingxuan.modules.comment.dto.AdminCommentVO;
import com.jingxuan.modules.comment.dto.CommentVO;
import com.jingxuan.modules.notification.service.NotificationService;
import com.jingxuan.modules.sensitive.service.DeepSeekReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentMatchers;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl - 评论服务")
class CommentServiceImplTest {

    @Mock private DeepSeekReviewService deepSeekReviewService;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysRoleMapper sysRoleMapper;
    @Mock private WorkMapper workMapper;
    @Mock private WorkMemberMapper workMemberMapper;
    @Mock private WorkPublishMapper workPublishMapper;
    @Mock private NotificationService notificationService;
    @Mock private WorkCommentMapper workCommentMapper;

    private CommentServiceImpl commentService;

    private static final Long WORK_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final Long ADMIN_ID = 300L;

    @BeforeEach
    void setUp() {
        commentService = new CommentServiceImpl(deepSeekReviewService, sysUserMapper, sysRoleMapper,
                workMapper, workMemberMapper, workPublishMapper, notificationService);
        ReflectionTestUtils.setField(commentService, "baseMapper", workCommentMapper);
    }

    @Nested
    @DisplayName("添加评论")
    class AddComment {

        @Test
        @DisplayName("成功添加评论")
        void shouldAddComment() {
            // given
            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.pass());
            Work work = new Work();
            work.setId(WORK_ID);
            work.setTitle("测试作品");
            when(workMapper.selectById(WORK_ID)).thenReturn(work);
            WorkPublish publish = new WorkPublish();
            publish.setPublishStatus(1);
            when(workPublishMapper.selectByWorkId(WORK_ID)).thenReturn(publish);
            when(workCommentMapper.insert(any(WorkComment.class))).thenAnswer(inv -> {
                ((WorkComment) inv.getArgument(0)).setId(100L);
                return 1;
            });

            // when
            Long commentId = commentService.addComment(WORK_ID, USER_ID, "好作品", null, null);

            // then
            assertNotNull(commentId);
            verify(workCommentMapper).insert(ArgumentMatchers.<WorkComment>argThat(c ->
                    c.getContent().equals("好作品") && c.getUserId().equals(USER_ID)
            ));
        }

        @Test
        @DisplayName("评论内容违规抛异常")
        void shouldThrowWhenContentViolates() {
            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.fail("abuse", "包含辱骂"));

            assertThrows(BusinessException.class,
                    () -> commentService.addComment(WORK_ID, USER_ID, "脏话", null, null));
        }

        @Test
        @DisplayName("未发布作品不可评论")
        void shouldThrowWhenWorkNotPublished() {
            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.pass());
            Work work = new Work();
            work.setId(WORK_ID);
            when(workMapper.selectById(WORK_ID)).thenReturn(work);
            when(workPublishMapper.selectByWorkId(WORK_ID)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> commentService.addComment(WORK_ID, USER_ID, "好作品", null, null));
        }

        @Test
        @DisplayName("回复评论时校验父评论存在")
        void shouldValidateParentComment() {
            when(deepSeekReviewService.review(anyString(), anyString()))
                    .thenReturn(DeepSeekReviewService.ReviewResult.pass());
            Work work = new Work();
            work.setId(WORK_ID);
            when(workMapper.selectById(WORK_ID)).thenReturn(work);
            WorkPublish publish = new WorkPublish();
            publish.setPublishStatus(1);
            when(workPublishMapper.selectByWorkId(WORK_ID)).thenReturn(publish);
            when(workCommentMapper.selectById(999L)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> commentService.addComment(WORK_ID, USER_ID, "回复", 999L, null));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaQueryWrapper<WorkComment> createMockLambdaQuery() {
        LambdaQueryWrapper mockQuery = mock(LambdaQueryWrapper.class);
        doReturn(mockQuery).when(mockQuery).eq(any(), any());
        doReturn(mockQuery).when(mockQuery).select((com.baomidou.mybatisplus.core.toolkit.support.SFunction) any(), any());
        return mockQuery;
    }

    @Nested
    @DisplayName("删除评论")
    class DeleteComment {

        @Test
        @DisplayName("作者本人可删除自己的评论")
        void shouldDeleteOwnComment() {
            WorkComment comment = new WorkComment();
            comment.setId(1L);
            comment.setWorkId(WORK_ID);
            comment.setUserId(USER_ID);
            when(workCommentMapper.selectById(1L)).thenReturn(comment);

            LambdaQueryWrapper<WorkComment> mockQuery = createMockLambdaQuery();
            try (MockedStatic<Wrappers> wrappersMock = mockStatic(Wrappers.class)) {
                wrappersMock.when(() -> Wrappers.lambdaQuery()).thenReturn(mockQuery);
                commentService.deleteComment(1L, USER_ID, "ROLE_STUDENT");
            }

            verify(workCommentMapper).deleteByIds(anySet());
        }

        @Test
        @DisplayName("管理员可删除任何评论")
        void shouldDeleteAsAdmin() {
            WorkComment comment = new WorkComment();
            comment.setId(1L);
            comment.setWorkId(WORK_ID);
            comment.setUserId(101L);
            when(workCommentMapper.selectById(1L)).thenReturn(comment);

            LambdaQueryWrapper<WorkComment> mockQuery = createMockLambdaQuery();
            try (MockedStatic<Wrappers> wrappersMock = mockStatic(Wrappers.class)) {
                wrappersMock.when(() -> Wrappers.lambdaQuery()).thenReturn(mockQuery);
                commentService.deleteComment(1L, ADMIN_ID, "ROLE_ADMIN");
            }

            verify(workCommentMapper).deleteByIds(anySet());
        }

        @Test
        @DisplayName("无权删除他人评论")
        void shouldThrowWhenNoPermission() {
            WorkComment comment = new WorkComment();
            comment.setId(1L);
            comment.setWorkId(WORK_ID);
            comment.setUserId(101L);
            when(workCommentMapper.selectById(1L)).thenReturn(comment);

            assertThrows(BusinessException.class,
                    () -> commentService.deleteComment(1L, USER_ID, "ROLE_STUDENT"));
        }

        @Test
        @DisplayName("评论不存在抛异常")
        void shouldThrowWhenNotFound() {
            when(workCommentMapper.selectById(anyLong())).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> commentService.deleteComment(1L, USER_ID, "ROLE_ADMIN"));
        }
    }

    @Nested
    @DisplayName("评论查询")
    class QueryComment {

        @Test
        @DisplayName("分页查询作品评论")
        void shouldGetWorkComments() {
            Page<WorkComment> mpPage = new Page<>(1, 10);
            mpPage.setRecords(List.of(new WorkComment()));
            mpPage.setTotal(1);
            when(workCommentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

            PageResult<WorkComment> result = commentService.getWorkComments(WORK_ID, 1, 10);
            assertEquals(1, result.getTotal());
        }

        @Test
        @DisplayName("查询管理端评论列表")
        void shouldGetAdminComments() {
            WorkComment comment = new WorkComment();
            comment.setId(1L);
            comment.setWorkId(WORK_ID);
            comment.setUserId(USER_ID);
            comment.setContent("好作品");

            Page<WorkComment> mpPage = new Page<>(1, 10);
            mpPage.setRecords(List.of(comment));
            mpPage.setTotal(1);
            when(workCommentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

            Work work = new Work();
            work.setId(WORK_ID);
            work.setTitle("测试作品");
            when(workMapper.selectBatchIds(anySet())).thenReturn(List.of(work));

            SysUser user = new SysUser();
            user.setId(USER_ID);
            user.setRealName("张三");
            user.setRoleId(1);
            when(sysUserMapper.selectBatchIds(anySet())).thenReturn(List.of(user));

            SysRole role = new SysRole();
            role.setId(1L);
            role.setRoleName("学生");
            when(sysRoleMapper.selectList(any())).thenReturn(List.of(role));

            PageResult<AdminCommentVO> result = commentService.getAdminComments(1, 10, null, null, null);

            assertEquals(1, result.getTotal());
            assertEquals("测试作品", result.getRecords().get(0).getWorkTitle());
            assertEquals("张三", result.getRecords().get(0).getUserName());
        }
    }
}
