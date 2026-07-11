import request from '../request'

export interface StudentTask {
  id: number
  userId: number
  batchId: number
  workId: number | null
  title: string
  content: string
  status: number // 0=待处理 1=已完成 2=已驳回 3=已截止
  createTime: string
  updateTime: string
  batchName?: string
  startTime?: string
  endTime?: string
}

/** 评分批次（用于批次选择弹窗） */
export interface BatchItem {
  id: number
  batchName: string
  startTime: string
  endTime: string
  status: number
}

export function getMyTasks() {
  return request.get('/student/tasks')
}

export function completeTask(taskId: number, workId: number) {
  return request.post(`/student/tasks/${taskId}/complete`, null, {
    params: { workId },
  })
}

/** 获取当前学生可参与的评分批次列表 */
export function getAvailableBatches() {
  return request.get('/student/batch/available')
}
