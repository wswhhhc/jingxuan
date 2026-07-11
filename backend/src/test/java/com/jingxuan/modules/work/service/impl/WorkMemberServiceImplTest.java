package com.jingxuan.modules.work.service.impl;

import com.jingxuan.mapper.WorkMemberMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * WorkMemberServiceImpl 使用 MyBatis-Plus lambdaQuery/lambdaUpdate，
 * 需要真实 Mapper 代理，Mock 环境下暂不测试。
 * 集成测试中使用 @SpringBootTest + H2 覆盖。
 */
@ExtendWith(MockitoExtension.class)
class WorkMemberServiceImplTest {

    @Mock private WorkMemberMapper workMemberMapper;

    // saveMembers/getByWorkId/removeByWorkId 均使用 lambdaQuery/lambdaUpdate，
    // 需要真实 Mapper 代理，Mock 环境下暂不覆盖
}
