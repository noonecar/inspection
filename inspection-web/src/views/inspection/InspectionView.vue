<template>
  <div class="page-card">
    <div class="page-title">检查执行</div>

    <el-tabs v-model="activeCategory" class="mb-12" @tab-change="onCategoryChange">
      <el-tab-pane v-for="item in categoryTabs" :key="item.value" :name="item.value">
        <template #label>
          <span>{{ item.label }}<el-badge v-if="getNewTaskCountForCategory(item) > 0" :value="getNewTaskCountForCategory(item)" :max="99" class="tab-badge" /></span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div class="toolbar mb-12">
      <div class="toolbar-left">
        <el-button @click="downloadRecords">导出记录</el-button>
      </div>

      <div class="toolbar-right">
        <el-input v-model="query.keyword" placeholder="检查内容/检查员" clearable class="basic-filter-input" />
        <el-select v-model="query.result" placeholder="检查结果" clearable class="basic-filter-select">
          <el-option label="合格" value="合格" />
          <el-option label="不合格" value="不合格" />
        </el-select>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>
    </div>

    <el-table :data="tableData" stripe border :row-class-name="getRowClassName">
      <el-table-column prop="taskName" label="任务名称" min-width="190" show-overflow-tooltip />
      <el-table-column label="检查对象" min-width="170">
        <template #default="scope">
          <template v-if="scope.row.stationIdList && scope.row.stationIdList.length > 0">
            <el-link
              v-for="(sid, idx) in scope.row.stationIdList"
              :key="sid"
              type="primary"
              @click="openStationDetail(sid)"
            >{{ stationMap[sid]?.name || '台站-' + sid }}</el-link
            ><template v-if="idx < scope.row.stationIdList.length - 1">、</template>
          </template>
          <span v-else>{{ scope.row.stationName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="inspectionMode" label="检查方式" width="100" />
      <el-table-column prop="itemName" label="检查内容" min-width="220" show-overflow-tooltip />
      <el-table-column prop="result" label="结果" width="90" />
      <el-table-column prop="inspector" label="检查员" width="110">
        <template #default="scope">{{ scope.row.inspectorDisplay || scope.row.inspector || '' }}</template>
      </el-table-column>
      <el-table-column label="检查时间" width="110">
        <template #default="scope">{{ formatDateOnly(scope.row.checkedAt) }}</template>
      </el-table-column>
      <el-table-column label="材料" min-width="240">
        <template #default="scope">
          <el-space wrap>
            <el-link
              v-for="(url, index) in splitEvidenceUrls(scope.row.evidenceUrls)"
              :key="`${scope.row.id}-${index}`"
              :href="resolveFileUrl(url)"
              target="_blank"
              type="primary"
            >
              {{ extractFileName(url) }}
            </el-link>
            <span v-if="!splitEvidenceUrls(scope.row.evidenceUrls).length" class="text-muted">无</span>
          </el-space>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="scope">
          <el-button v-if="scope.row.virtual && canEdit" size="small" text type="primary" @click="openDialogForTask(scope.row.taskId)">录入</el-button>
          <el-button v-if="!scope.row.virtual && canEdit" size="small" text type="primary" @click="openDialog(scope.row)">编辑</el-button>
          <el-button v-if="!scope.row.virtual && canEdit && scope.row.taskStatus === '待复检'" size="small" text type="warning" @click="openReinspection(scope.row)">复检</el-button>
          <el-button v-if="!scope.row.virtual && canEdit" size="small" text type="info" @click="openResultView(scope.row)">查看结果</el-button>
          <el-button v-if="!scope.row.virtual && canDelete" size="small" text type="danger" @click="deleteRecord(scope.row)">删除</el-button>
        </template>
      </el-table-column>

      <el-table-column label="轮次" width="70" align="center">
        <template #default="scope">
          <el-tag v-if="!scope.row.virtual && scope.row.round > 1" type="warning" size="small">第{{ scope.row.round }}轮</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="mt-12"
      background
      layout="total, prev, pager, next"
      :total="total"
      v-model:current-page="query.page"
      :page-size="query.size"
      @current-change="load"
    />

    <el-dialog v-model="visible" title="录入检查记录" width="960px">
      <el-form :model="form" label-width="122px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="关联任务">
              <el-input v-model="form.taskName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检查对象">
              <el-input v-model="form.stationName" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="检查方式"><el-input v-model="form.inspectionMode" disabled /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="检查员"><el-input v-model="form.inspectorDisplay" disabled /></el-form-item></el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="检查时间"><el-date-picker v-model="form.checkedAt" type="date" value-format="YYYY-MM-DD" :disabled-date="disabledPastDate" /></el-form-item></el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="检查结论"><el-select v-model="form.isQualified" placeholder="请选择"><el-option label="合格" :value="true" /><el-option label="不合格" :value="false" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="整改期限" v-if="form.isQualified === false"><el-date-picker v-model="form.rectificationDeadline" type="date" value-format="YYYY-MM-DD" :disabled-date="disabledPastDate" /></el-form-item></el-col>
        </el-row>

        <el-form-item label="不合格原因" v-if="form.isQualified === false"><el-input v-model="form.noQualifiedReason" /></el-form-item>

        <el-divider>检查明细</el-divider>
        <el-tabs v-if="form.stationIdList && form.stationIdList.length > 1" v-model="activeStationTab" type="card" class="mb-12">
          <el-tab-pane v-for="sid in form.stationIdList" :key="sid" :label="stationMap[sid]?.name || `台站-${sid}`" :name="String(sid)" />
        </el-tabs>
        <el-table :data="detailTableData" size="small" border class="detail-table" :header-cell-style="{ background: '#f5f7fa' }">
          <el-table-column label="检查项目" prop="label" width="260" show-overflow-tooltip />
          <el-table-column label="实际值" min-width="200">
            <template #default="{ row }">
              <template v-if="row.meta.type === 'number'">
                <el-input-number
                  v-model="form[row.key]"
                  size="small"
                  :step="row.meta.step || 1"
                  :controls="false"
                  :disabled="isDetailLocked(row.key)"
                  class="detail-table-input"
                  @change="() => updateDetailResult(row.key)"
                />
                <span class="detail-inline-unit">{{ row.meta.unit }}</span>
              </template>
              <template v-else-if="row.meta.type === 'money'">
                <el-input-number
                  v-model="form[row.key]"
                  size="small"
                  :step="row.meta.step || 1"
                  :controls="false"
                  :precision="2"
                  :disabled="isDetailLocked(row.key)"
                  class="detail-table-input"
                  @change="() => updateDetailResult(row.key)"
                />
                <span class="detail-inline-unit">{{ row.meta.unit }}</span>
              </template>
              <template v-else-if="row.meta.type === 'percent'">
                <el-input-number
                  v-model="form[row.key]"
                  size="small"
                  :step="row.meta.step || 0.1"
                  :controls="false"
                  :min="0"
                  :max="100"
                  :disabled="isDetailLocked(row.key)"
                  class="detail-table-input"
                  @change="() => updateDetailResult(row.key)"
                />
                <span class="detail-inline-unit">{{ row.meta.unit }}</span>
              </template>
              <template v-else-if="row.meta.type === 'select'">
                <el-select v-model="form[row.key]" size="small" :placeholder="row.meta.placeholder" :disabled="isDetailLocked(row.key)" class="detail-table-select" @change="() => updateDetailResult(row.key)">
                  <el-option v-for="opt in row.meta.options" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </template>
              <template v-else-if="row.meta.type === 'date'">
                <el-date-picker v-model="form[row.key]" size="small" type="date" value-format="YYYY-MM-DD" :placeholder="row.meta.placeholder" :disabled="isDetailLocked(row.key)" class="detail-table-select" @change="() => updateDetailResult(row.key)" />
              </template>
              <template v-else>
                <el-input v-model="form[row.key]" size="small" :placeholder="row.meta.placeholder" :disabled="isDetailLocked(row.key)" class="detail-table-input" @input="(val) => { form[row.key] = filterInputValue(val, row); updateDetailResult(row.key) }" />
              </template>
            </template>
          </el-table-column>
          <el-table-column label="检查结论" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getAutoResultTagType(form[row.resultKey])" size="small" effect="plain" class="auto-result-tag">
                {{ form[row.resultKey] || '未判定' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <el-form-item label="过程记录"><el-input v-model="form.processRecord" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="证据材料">
          <el-space wrap>
            <el-upload :show-file-list="false" :auto-upload="false" :on-change="uploadEvidence">
              <el-button>上传证据</el-button>
            </el-upload>
            <el-tag v-for="(url, index) in splitEvidenceUrls(form.evidenceUrls)" :key="`file-${index}`" closable @close="removeEvidenceAt(index)">
              <el-link :href="resolveFileUrl(url)" target="_blank" type="primary">{{ extractFileName(url) }}</el-link>
            </el-tag>
            <span v-if="!splitEvidenceUrls(form.evidenceUrls).length" class="text-muted">暂未上传材料</span>
          </el-space>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 检查结果查看对话框 -->
    <el-dialog v-model="resultVisible" title="检查结果" width="1100px" top="5vh">
      <div ref="resultPdfRef" class="result-pdf-content">
        <!-- 任务信息 -->
        <el-descriptions :column="3" border size="small" class="result-section">
          <el-descriptions-item label="任务名称" :span="2">{{ resultTaskInfo.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="检查方式">{{ resultTaskInfo.inspectionMode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="检查对象" :span="2">{{ resultTaskInfo.stationName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="检查员">{{ resultTaskInfo.inspector || '-' }}</el-descriptions-item>
          <el-descriptions-item label="检查时间">{{ formatDateOnly(resultTaskInfo.checkedAt) || '-' }}</el-descriptions-item>
        </el-descriptions>

      <!-- 台站信息 -->
      <el-divider content-position="left">台站信息</el-divider>
      <el-descriptions :column="3" border size="small" class="result-section">
        <el-descriptions-item label="台站名称">{{ resultStation?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="识别码/呼号">{{ resultStation?.stationCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="台站分类">{{ resultStation?.stationClass || '-' }}</el-descriptions-item>
        <el-descriptions-item label="台站类型">{{ resultStation?.stationType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ resultStation?.serviceType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执照号">{{ resultStation?.licenseNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="有效期">{{ formatDateOnly(resultStation?.validUntil) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="台址/使用区域" :span="2">{{ resultStation?.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="经度">{{ resultStation?.longitude ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="纬度">{{ resultStation?.latitude ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="发射功率(dBm)">{{ resultStation?.transmitPower ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="占用带宽(kHz)">{{ resultStation?.occupiedBandwidth ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="天线增益(dBi)">{{ resultStation?.antennaGain ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="极化方式">{{ resultStation?.polarization || '-' }}</el-descriptions-item>
        <el-descriptions-item label="天线距地高度(m)">{{ resultStation?.antennaHeight ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="天线尺寸(m)">{{ resultStation?.antennaSize ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="最大EIRP(dBm)">{{ resultStation?.maxEirp ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="轨道信息">{{ resultStation?.orbitalPosition || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总带宽(kHz)">{{ resultStation?.totalBandwidth ?? '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 检查明细 -->
      <el-divider content-position="left">检查明细</el-divider>
      <el-tabs v-if="resultStationIdList.length > 1" v-model="resultActiveStation" type="card" size="small" class="mb-12">
        <el-tab-pane v-for="sid in resultStationIdList" :key="sid" :label="stationMap[sid]?.name || `台站-${sid}`" :name="String(sid)" />
      </el-tabs>
      <el-table :data="resultDetailRows" size="small" border class="detail-table" :header-cell-style="{ background: '#f5f7fa' }">
        <el-table-column label="检查项目" prop="label" width="260" show-overflow-tooltip />
        <el-table-column label="实际值" min-width="200">
          <template #default="{ row }">
            {{ formatResultValue(resultDetailData[row.key], row.meta) }}
          </template>
        </el-table-column>
        <el-table-column label="检查结论" width="100" align="center">
          <template #default="{ row }">
            <div class="pdf-tag-wrap">
              <template v-if="isPdfExporting">
                <span :class="['pdf-tag-text', `pdf-tag-${getAutoResultTagType(resultDetailData[row.resultKey])}`]">
                  {{ resultDetailData[row.resultKey] || '未判定' }}
                </span>
              </template>
              <el-tag v-else :type="getAutoResultTagType(resultDetailData[row.resultKey])" size="small" effect="plain" class="pdf-tag">
                {{ resultDetailData[row.resultKey] || '未判定' }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 检查结论 -->
      <el-divider content-position="left">检查结论</el-divider>
      <el-descriptions :column="2" border size="small" class="result-section">
        <el-descriptions-item label="结论">
          <template v-if="isPdfExporting">
            <span :class="['pdf-tag-text', isResultQualified(resultDetailData.isQualified) ? 'pdf-tag-success' : 'pdf-tag-danger']">
              {{ isResultQualified(resultDetailData.isQualified) ? '合格' : '不合格' }}
            </span>
          </template>
          <el-tag v-else :type="isResultQualified(resultDetailData.isQualified) ? 'success' : 'danger'" size="small" class="pdf-tag">
            {{ isResultQualified(resultDetailData.isQualified) ? '合格' : '不合格' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="检查员">{{ resultTaskInfo.inspector || '-' }}</el-descriptions-item>
        <el-descriptions-item label="检查时间">{{ formatDateOnly(resultTaskInfo.checkedAt) || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="!isResultQualified(resultDetailData.isQualified)" label="整改期限">{{ resultDetailData.rectificationDeadline || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="!isResultQualified(resultDetailData.isQualified)" label="不合格原因" :span="2">{{ resultDetailData.noQualifiedReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="过程记录" :span="2">{{ resultTaskInfo.processRecord || '-' }}</el-descriptions-item>
        <el-descriptions-item label="证据材料" :span="2">
          <el-space wrap>
            <el-link
              v-for="(url, index) in splitEvidenceUrls(resultTaskInfo.evidenceUrls)"
              :key="`result-file-${index}`"
              :href="resolveFileUrl(url)"
              target="_blank"
              type="primary"
            >
              {{ extractFileName(url) }}
            </el-link>
            <span v-if="!splitEvidenceUrls(resultTaskInfo.evidenceUrls).length" class="text-muted">暂未上传材料</span>
          </el-space>
        </el-descriptions-item>
      </el-descriptions>
      </div>

      <template #footer>
        <el-button type="primary" :loading="downloadingPdf" @click="downloadResultPdf">下载PDF</el-button>
        <el-button @click="resultVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <StationDetailDialog v-model="stationDetailVisible" :station-id="selectedStationId" />

  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import { exportInspections, fetchInspections, deleteInspection } from '../../api/inspections'
import { fetchInspectionDetail, saveInspectionDetail } from '../../api/inspectionDetails'
import { fetchInspectors, fetchTasks } from '../../api/tasks'
import { fetchStations } from '../../api/stations'
import { fetchFrequencies } from '../../api/frequencies'
import { uploadFile } from '../../api/files'
import { useAuthStore } from '../../stores/auth'
import StationDetailDialog from '../../components/StationDetailDialog.vue'

const categoryTabs = [
  { label: '地面频率使用', value: '地面频率使用', taskType: '频率检查任务', objectType: '无线电频率', detailKey: 'ground-frequency' },
  { label: '卫星频率使用', value: '卫星频率使用', taskType: '频率检查任务', objectType: '无线电频率', detailKey: 'satellite-frequency' },
  { label: '卫星通信网频率', value: '卫星通信网频率', taskType: '频率检查任务', objectType: '无线电频率', detailKey: 'satellite-network' },
  { label: '地面台站', value: '地面台站', taskType: '台站检查任务', objectType: '在用无线电台（站）', detailKey: 'ground-station' },
  { label: '空间电台', value: '空间电台', taskType: '台站检查任务', objectType: '在用无线电台（站）', detailKey: 'space-station' },
  { label: '卫星地球站', value: '卫星地球站', taskType: '台站检查任务', objectType: '在用无线电台（站）', detailKey: 'satellite-earth-station' }
]

const detailFieldMap = {
  地面频率使用: [
    { key: 'check_1_1_1_1', label: '1.1 使用频率' },
    { key: 'check_1_1_1_2', label: '1.2 使用地域' },
    { key: 'check_1_1_1_3', label: '1.3 业务用途' },
    { key: 'check_1_1_1_4', label: '1.4 使用期限' },
    { key: 'check_1_1_1_5', label: '1.5 使用率' },
    { key: 'check_1_1_1_6_1', label: '1.6.1 特别规定事项-发射设备型号核准' },
    { key: 'check_1_1_1_6_2', label: '1.6.2 特别规定事项-边境协调协议' },
    { key: 'check_1_1_1_6_3', label: '1.6.3 特别规定事项-其他' },
    { key: 'check_1_1_2', label: '2 技术方案条件' },
    { key: 'check_1_1_3', label: '3 专业技术人员' },
    { key: 'check_1_1_4', label: '4 有害干扰' },
    { key: 'check_1_1_5', label: '5 伪造/冒用许可' },
    { key: 'check_1_1_6', label: '6 许可2年内是否使用' },
    { key: 'check_1_1_7', label: '7 频率占用费缴纳' },
    { key: 'check_1_1_8', label: '8 年度报告报送' },
    { key: 'check_1_1_9', label: '9 其他规定' }
  ],
  卫星频率使用: [
    { key: 'check_1_2_1_1', label: '1.1 使用频率' },
    { key: 'check_1_2_1_2', label: '1.2 使用地域' },
    { key: 'check_1_2_1_3', label: '1.3 业务用途' },
    { key: 'check_1_2_1_4', label: '1.4 使用期限' },
    { key: 'check_1_2_1_5', label: '1.5 使用率' },
    { key: 'check_1_2_1_6_1', label: '1.6.1 卫星网络资料' },
    { key: 'check_1_2_1_6_2', label: '1.6.2 国际/国内协调' },
    { key: 'check_1_2_1_6_3', label: '1.6.3 EIRP谱密度' },
    { key: 'check_1_2_1_6_4', label: '1.6.4 无线电规则21.16' },
    { key: 'check_1_2_1_6_5', label: '1.6.5 其他规定' },
    { key: 'check_1_2_2', label: '2 技术方案条件' },
    { key: 'check_1_2_3', label: '3 专业技术人员' },
    { key: 'check_1_2_4', label: '4 有害干扰' },
    { key: 'check_1_2_5', label: '5 伪造/冒用许可' },
    { key: 'check_1_2_6', label: '6 许可2年内是否使用' },
    { key: 'check_1_2_7', label: '7 频率占用费缴纳' },
    { key: 'check_1_2_8', label: '8 年度报告报送' },
    { key: 'check_1_2_9', label: '9 其他规定' }
  ],
  卫星通信网频率: [
    { key: 'check_1_3_1_1', label: '1.1 使用频率' },
    { key: 'check_1_3_1_2', label: '1.2 使用地域' },
    { key: 'check_1_3_1_3', label: '1.3 业务用途' },
    { key: 'check_1_3_1_4', label: '1.4 使用期限' },
    { key: 'check_1_3_1_5', label: '1.5 使用率' },
    { key: 'check_1_3_1_6_1', label: '1.6.1 主站天线尺寸' },
    { key: 'check_1_3_1_6_2', label: '1.6.2 主站EIRP谱密度' },
    { key: 'check_1_3_1_6_3', label: '1.6.3 终端地球站情况' },
    { key: 'check_1_3_1_6_4', label: '1.6.4 网内地球站执照办理' },
    { key: 'check_1_3_1_6_5', label: '1.6.5 其他规定' },
    { key: 'check_1_3_2', label: '2 技术方案条件' },
    { key: 'check_1_3_3', label: '3 专业技术人员' },
    { key: 'check_1_3_4', label: '4 有害干扰' },
    { key: 'check_1_3_5', label: '5 伪造/冒用许可' },
    { key: 'check_1_3_6', label: '6 许可2年内是否使用' },
    { key: 'check_1_3_7', label: '7 频率占用费缴纳' },
    { key: 'check_1_3_8', label: '8 年度报告报送' },
    { key: 'check_1_3_9', label: '9 其他规定' }
  ],
  地面台站: [
    { key: 'check_2_1_1_1', label: '1.1 有效期' },
    { key: 'check_2_1_1_2_1', label: '1.2.1 无线电台识别码' },
    { key: 'check_2_1_1_2_2', label: '1.2.2 台址/使用区域' },
    { key: 'check_2_1_1_2_3', label: '1.2.3 地理坐标' },
    { key: 'check_2_1_1_3_1', label: '1.3.1 发射频点/频率范围' },
    { key: 'check_2_1_1_3_2', label: '1.3.2 发射功率' },
    { key: 'check_2_1_1_3_3', label: '1.3.3 占用带宽' },
    { key: 'check_2_1_1_3_4', label: '1.3.4 其他必要信息' },
    { key: 'check_2_1_1_4_1', label: '1.4.1 发射设备型号核准代码' },
    { key: 'check_2_1_1_4_2', label: '1.4.2 天线增益' },
    { key: 'check_2_1_1_4_3', label: '1.4.3 极化方式' },
    { key: 'check_2_1_1_4_4', label: '1.4.4 天线距地高度' },
    { key: 'check_2_1_1_5_1', label: '1.5.1 业余无线电台操作证书' },
    { key: 'check_2_1_1_5_2', label: '1.5.2 是否向ITU申报' },
    { key: 'check_2_1_1_5_3', label: '1.5.3 边境频率协调' },
    { key: 'check_2_1_1_5_4', label: '1.5.4 3000-5000MHz基站协调' },
    { key: 'check_2_1_1_5_5', label: '1.5.5 其他规定' },
    { key: 'check_2_1_2', label: '2 技术方案条件' },
    { key: 'check_2_1_3', label: '3 专业技术人员' },
    { key: 'check_2_1_4', label: '4 有害干扰' },
    { key: 'check_2_1_5', label: '5 许可事项外信号' },
    { key: 'check_2_1_6', label: '6 识别码/呼号使用' },
    { key: 'check_2_1_7', label: '7 定期维护' },
    { key: 'check_2_1_8', label: '8 通联日志' },
    { key: 'check_2_1_9', label: '9 频率占用费缴纳' },
    { key: 'check_2_1_10', label: '10 其他规定' }
  ],
  空间电台: [
    { key: 'check_2_2_1_1', label: '1.1 有效期' },
    { key: 'check_2_2_1_2_1', label: '1.2.1 用途' },
    { key: 'check_2_2_1_2_2', label: '1.2.2 业务类别' },
    { key: 'check_2_2_1_2_3', label: '1.2.3 轨道信息' },
    { key: 'check_2_2_1_3_1', label: '1.3.1 频率范围' },
    { key: 'check_2_2_1_3_2', label: '1.3.2 极化方式' },
    { key: 'check_2_2_1_3_3', label: '1.3.3 占用带宽' },
    { key: 'check_2_2_1_3_4', label: '1.3.4 最大EIRP' },
    { key: 'check_2_2_1_4_1', label: '1.4.1 向ITU申报' },
    { key: 'check_2_2_1_4_2', label: '1.4.2 国内/国际协调' },
    { key: 'check_2_2_1_4_3', label: '1.4.3 干扰规避措施' },
    { key: 'check_2_2_1_4_4', label: '1.4.4 其他规定' },
    { key: 'check_2_2_2', label: '2 技术方案条件' },
    { key: 'check_2_2_3', label: '3 专业技术人员' },
    { key: 'check_2_2_4', label: '4 有害干扰' },
    { key: 'check_2_2_5', label: '5 许可事项外信号' },
    { key: 'check_2_2_6', label: '6 频率占用费缴纳' },
    { key: 'check_2_2_7', label: '7 其他规定' }
  ],
  卫星地球站: [
    { key: 'check_2_3_1_1', label: '1.1 有效期' },
    { key: 'check_2_3_1_2_1', label: '1.2.1 卫星地球站类型' },
    { key: 'check_2_3_1_2_2', label: '1.2.2 卫星地球站用途' },
    { key: 'check_2_3_1_2_3', label: '1.2.3 站址/使用区域' },
    { key: 'check_2_3_1_2_4', label: '1.2.4 地理坐标' },
    { key: 'check_2_3_1_2_5', label: '1.2.5 发射设备型号' },
    { key: 'check_2_3_1_2_6', label: '1.2.6 天线增益' },
    { key: 'check_2_3_1_2_7', label: '1.2.7 天线尺寸' },
    { key: 'check_2_3_1_2_8', label: '1.2.8 天线距地高度' },
    { key: 'check_2_3_1_3_1', label: '1.3.1 频率范围' },
    { key: 'check_2_3_1_3_2', label: '1.3.2 极化方式' },
    { key: 'check_2_3_1_3_3', label: '1.3.3 占用带宽' },
    { key: 'check_2_3_1_3_4', label: '1.3.4 发射功率' },
    { key: 'check_2_3_1_3_5', label: '1.3.5 总带宽' },
    { key: 'check_2_3_1_4_1', label: '1.4.1 向ITU申报' },
    { key: 'check_2_3_1_4_2', label: '1.4.2 国内/国际协调' },
    { key: 'check_2_3_1_4_3', label: '1.4.3 干扰规避措施' },
    { key: 'check_2_3_1_4_4', label: '1.4.4 频率/轨道协同' },
    { key: 'check_2_3_1_4_5', label: '1.4.5 其他规定' },
    { key: 'check_2_3_2', label: '2 技术方案条件' },
    { key: 'check_2_3_3', label: '3 专业技术人员' },
    { key: 'check_2_3_4', label: '4 有害干扰' },
    { key: 'check_2_3_5', label: '5 许可事项外信号' },
    { key: 'check_2_3_6', label: '6 频率占用费缴纳' },
    { key: 'check_2_3_7', label: '7 定期维护' },
    { key: 'check_2_3_8', label: '8 其他规定' }
  ]
}

const activeCategory = ref('地面频率使用')

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  result: ''
})

const tableData = ref([])
const total = ref(0)

const taskOptions = ref([])
const taskMap = ref({})
const stationOptions = ref([])
const stationMap = ref({})
const inspectorMap = ref({})

const stationDetailVisible = ref(false)
const selectedStationId = ref(null)

const visible = ref(false)
const currentFrequency = ref(null)
const frequencyCache = ref({})

const auth = useAuthStore()
const role = computed(() => auth.user?.role || 'OPERATOR')
const canEdit = computed(() => ['ADMIN', 'OPERATOR', 'INSPECTOR'].includes(role.value))
const canDelete = computed(() => role.value === 'ADMIN')

const form = reactive({
  taskId: null,
  stationId: null,
  stationIdList: [],
  taskName: '',
  stationName: '',
  inspectionMode: '日常检查',
  inspectorName: '',
  inspectorDisplay: '',
  checkedAt: '',
  isQualified: true,
  noQualifiedReason: '',
  rectificationDeadline: '',
  processRecord: '',
  evidenceUrls: '',
  remarks: ''
})

const activeStationTab = ref('')
const multiStationCache = ref({})
const reinspectionMode = ref(false)
const reinspectionLocks = ref({})

// 检查结果查看对话框状态
const resultVisible = ref(false)
const resultTaskInfo = ref({})
const resultStation = ref(null)
const resultStationIdList = ref([])
const resultActiveStation = ref('')
const resultDetailData = ref({})
const resultDetailCache = ref({})
const resultPdfRef = ref(null)
const downloadingPdf = ref(false)
const isPdfExporting = ref(false)

const detailFields = computed(() => detailFieldMap[getCurrentMeta().value] || [])

const detailTableData = computed(() => detailFields.value.map(field => ({
  key: field.key,
  label: field.label,
  resultKey: getResultKey(field),
  meta: getFieldMeta(field)
})))

const resultDetailRows = computed(() => detailTableData.value)

const formatResultValue = (value, meta) => {
  if (value === null || value === undefined || value === '') return '-'
  if (meta.type === 'money') return `${value} 元`
  if (meta.type === 'percent') return `${value}%`
  if (meta.type === 'number') return `${value} ${meta.unit || ''}`.trim()
  return String(value)
}

const isResultQualified = (val) => val === true || val === 1 || val === '1' || val === '合格'

const openResultView = async (row) => {
  const task = taskMap.value[row.taskId] || {}
  resultTaskInfo.value = {
    taskId: row.taskId,
    name: task.name || `任务-${row.taskId}`,
    stationName: '',
    inspectionMode: row.inspectionMode || task.inspectionMode || '',
    inspector: resolveInspectorNames(row.inspector || task.assignee),
    checkedAt: formatDateOnly(row.checkedAt),
    processRecord: row.processRecord || '',
    evidenceUrls: row.evidenceUrls || ''
  }

  // 加载多台站任务的台站 ID 列表
  let stationIds = []
  if (task.stationIds) {
    try { stationIds = JSON.parse(task.stationIds) } catch (e) { /* ignore */ }
  }
  if (!stationIds.length && task.stationId) stationIds = [task.stationId]
  resultStationIdList.value = stationIds
  if (stationIds.length > 0) {
    resultActiveStation.value = String(stationIds[0])
  }

  // 更新所有台站的名称
  resultTaskInfo.value.stationName = stationIds.map(sid => stationMap.value[sid]?.name || `台站-${sid}`).join('、')

  // 预加载所有台站的检查明细数据
  resultDetailCache.value = {}
  for (const sid of stationIds) {
    await loadResultDetail(row.taskId, sid)
  }
  // 默认显示第一个台站
  if (resultDetailCache.value[stationIds[0]]) {
    resultDetailData.value = resultDetailCache.value[stationIds[0]]
  }
  resultStation.value = stationMap.value[stationIds[0]] || null

  resultVisible.value = true
}

const loadResultDetail = async (taskId, stationId) => {
  const meta = getCurrentMeta()
  if (!taskId || !meta.detailKey) return
  const sid = stationId || resultStationIdList.value[0]
  const res = await fetchInspectionDetail(meta.detailKey, { taskId, stationId: sid })
  if (res.success) {
    resultDetailData.value = res.data || {}
    if (resultDetailData.value.isQualified !== true && resultDetailData.value.isQualified !== false) {
      const raw = resultDetailData.value.isQualified
      resultDetailData.value.isQualified = raw === true || raw === 1 || raw === '1' || raw === 'true' || raw === '合格'
    }
    resultDetailCache.value[sid] = { ...resultDetailData.value }
  }
}

const getFieldMeta = (field) => {
  const key = field.key
  const label = field.label || ''
  const lower = label.toLowerCase()
  const selectYesNo = [
    { label: '是', value: '是' },
    { label: '否', value: '否' }
  ]
  const selectHasNo = [
    { label: '有', value: '有' },
    { label: '无', value: '无' }
  ]
  const selectReported = [
    { label: '已报送', value: '已报送' },
    { label: '未报送', value: '未报送' }
  ]
  const selectQualified = [
    { label: '符合', value: '符合' },
    { label: '不符合', value: '不符合' }
  ]

  if (['check_1_1_7', 'check_1_2_7', 'check_1_3_7', 'check_2_1_9', 'check_2_2_6', 'check_2_3_6'].includes(key)) {
    return { type: 'money', unit: '元', step: 1, placeholder: '缴纳金额' }
  }
  if (['check_1_1_8', 'check_1_2_8', 'check_1_3_8'].includes(key)) {
    const custom = customSelectOptions[key]
    return { type: 'select', options: custom || selectReported, placeholder: '选择报送情况' }
  }
  if (['check_1_1_1_4', 'check_1_2_1_4', 'check_1_3_1_4', 'check_2_1_1_1', 'check_2_2_1_1', 'check_2_3_1_1'].includes(key)) {
    return { type: 'date', placeholder: 'YYYY-MM-DD' }
  }
  if (['check_1_1_1_5', 'check_1_2_1_5', 'check_1_3_1_5'].includes(key)) {
    return { type: 'percent', unit: '%', step: 0.1, placeholder: '使用率' }
  }
  if (['check_1_1_1_1', 'check_1_2_1_1', 'check_1_3_1_1', 'check_2_1_1_3_1', 'check_2_2_1_3_1', 'check_2_3_1_3_1'].includes(key)) {
    return { type: 'text', unit: '', placeholder: '频率范围/频点', inputRule: 'frequency' }
  }
  // 任何有自定义选项的字段都应当作 select，无论其标签是否匹配下方模式
  if (customSelectOptions[key]) {
    return { type: 'select', options: customSelectOptions[key], placeholder: '请选择' }
  }
  if (label.includes('有害干扰')) {
    return { type: 'select', options: customSelectOptions[key] || selectHasNo, placeholder: '是否存在' }
  }
  if (label.includes('是否') || label.includes('申报') || label.includes('协调') || label.includes('核准')) {
    return { type: 'select', options: customSelectOptions[key] || selectYesNo, placeholder: '请选择' }
  }
  if (label.includes('维护') || label.includes('日志') || label.includes('技术方案') || label.includes('专业技术人员')) {
    return { type: 'select', options: customSelectOptions[key] || selectQualified, placeholder: '请选择' }
  }
  if (lower.includes('功率') || lower.includes('eirp')) return { type: 'number', unit: 'dBm', step: 0.1, placeholder: '数值' }
  if (label.includes('带宽')) return { type: 'number', unit: 'kHz', step: 0.01, placeholder: '数值' }
  if (label.includes('增益')) return { type: 'number', unit: 'dBi', step: 0.1, placeholder: '数值' }
  if (label.includes('高度') || label.includes('距地') || label.includes('尺寸')) return { type: 'number', unit: 'm', step: 0.1, placeholder: '数值' }
  if (label.includes('坐标')) return { type: 'text', unit: '', placeholder: '经度,纬度', inputRule: 'coordinate' }
  if (label.includes('地域') || label.includes('地址') || label.includes('用途') || label.includes('台址') || label.includes('区域')) return { type: 'text', unit: '', placeholder: '实际情况', inputRule: 'chinese' }
  return { type: 'text', unit: '', placeholder: '实际情况', inputRule: 'free' }
}
const allDetailKeys = Array.from(new Set(Object.values(detailFieldMap).flat().map((item) => item.key)))

const getFieldMetaByKey = (fieldKey) => {
  for (const fields of Object.values(detailFieldMap)) {
    const found = fields.find(f => f.key === fieldKey)
    if (found) return found
  }
  return null
}
const allResultKeys = allDetailKeys.map((key) => `${key}_result`)

const resetDetailFields = () => {
  allDetailKeys.forEach((key) => {
    form[key] = ''
  })
  allResultKeys.forEach((key) => {
    form[key] = ''
  })
}

const getCurrentMeta = () => categoryTabs.find((item) => item.value === activeCategory.value) || categoryTabs[0]

const getResultKey = (field) => `${field.key}_result`

const isQualifiedResultValue = (value) => {
  if (value === true || value === 1 || value === '1') return true
  const text = String(value || '').trim()
  return text === '合格' || text === '符合' || text === 'true'
}

const setReinspectionLocks = (stationId, detailData) => {
  if (!reinspectionMode.value || !stationId || !detailData) return
  const locked = new Set()
  allDetailKeys.forEach((key) => {
    const resultValue = detailData[`${key}_result`]
    if (isQualifiedResultValue(resultValue)) {
      locked.add(key)
    }
  })
  reinspectionLocks.value[stationId] = locked
}

const isDetailLocked = (fieldKey) => {
  if (!reinspectionMode.value) return false
  const sid = form.stationIdList.length > 1 ? Number(activeStationTab.value) : form.stationId
  const locked = reinspectionLocks.value[sid]
  return locked ? locked.has(fieldKey) : false
}

const getAutoResultTagType = (result) => {
  if (result === '合格') return 'success'
  if (result === '不合格') return 'danger'
  return 'info'
}

const disabledPastDate = (time) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}

const formatDateOnly = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 10)
}

const formatDateTime = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

const splitEvidenceUrls = (value) => {
  if (!value) return []
  return String(value)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

const resolveFileUrl = (url) => {
  if (!url) return '#'
  if (/^https?:\/\//i.test(url)) return url
  return `http://localhost:8080${url.startsWith('/') ? '' : '/'}${url}`
}

const extractFileName = (url) => {
  if (!url) return ''
  const cleaned = String(url).split('?')[0]
  const parts = cleaned.split('/').filter(Boolean)
  return parts.length ? parts[parts.length - 1] : cleaned
}

const removeEvidenceAt = (index) => {
  const urls = splitEvidenceUrls(form.evidenceUrls)
  urls.splice(index, 1)
  form.evidenceUrls = urls.join(',')
}

const filterInputValue = (value, row) => {
  if (!value) return ''
  const rule = row.meta?.inputRule || 'free'
  if (rule === 'chinese') {
    return value.replace(/[^一-龥　-〿＀-￯\s]/g, '')
  }
  if (rule === 'coordinate') {
    return value.replace(/[^\d.,\-\s]/g, '')
  }
  if (rule === 'frequency') {
    return value.replace(/[^a-zA-Z0-9.\-\sGHzmk至~–—]/g, '')
  }
  return value
}

const loadBaseOptions = async () => {
  const [taskResult, stationResult, inspectorResult] = await Promise.allSettled([
    fetchTasks({ page: 1, size: 1000 }),
    fetchStations({ page: 1, size: 1000 }),
    fetchInspectors()
  ])

  const taskRes = taskResult.status === 'fulfilled' ? taskResult.value : null
  const stationRes = stationResult.status === 'fulfilled' ? stationResult.value : null
  const inspectorRes = inspectorResult.status === 'fulfilled' ? inspectorResult.value : null

  if (taskRes?.success) {
    const records = Array.isArray(taskRes.data) ? taskRes.data : taskRes.data.records
    taskOptions.value = records || []
    const map = {}
    ;(records || []).forEach((item) => {
      map[item.id] = item
    })
    taskMap.value = map
  }

  if (stationRes?.success) {
    const records = Array.isArray(stationRes.data) ? stationRes.data : stationRes.data.records
    stationOptions.value = records || []
    const map = {}
    ;(records || []).forEach((item) => {
      map[item.stationId] = item
    })
    stationMap.value = map
  }

  if (inspectorRes?.success) {
    const rows = Array.isArray(inspectorRes.data) ? inspectorRes.data : []
    const map = {}
    rows.forEach((item) => {
      map[item.username] = item.realName || ''
    })
    inspectorMap.value = map
  }
}

const resolveInspectorNames = (raw) => {
  if (!raw) return ''
  return String(raw).split(',').map(u => {
    const trimmed = u.trim()
    // 优先按用户名查找，找不到则原样返回（已是显示名称）
    return inspectorMap.value[trimmed] || trimmed
  }).filter(Boolean).join('、')
}

const openStationDetail = (stationId) => {
  selectedStationId.value = stationId
  stationDetailVisible.value = true
}

// 将原始分配检查员字符串解析为显示名称并存储
const resolveInspectorDisplay = (raw) => {
  if (!raw) return ''
  return String(raw).split(',').map(u => {
    const trimmed = u.trim()
    const realName = inspectorMap.value[trimmed]
    return realName || trimmed
  }).filter(Boolean).join('、')
}

const normalizeRow = (item) => {
  const task = taskMap.value[item.taskId] || {}
  return {
    ...item,
    virtual: false,
    taskName: task.name || `任务-${item.taskId || ''}`,
    taskStatus: task.status || '',
    stationName: stationMap.value[item.stationId]?.name || `台站-${item.stationId || ''}`,
    stationIdList: item.stationId ? [item.stationId] : [],
    inspectorDisplay: resolveInspectorNames(item.inspector),
    round: item.round || 1
  }
}

const buildVirtualRow = (task) => {
  let stationName = ''
  let stationIds = [task.stationId]
  if (task.stationIds) {
    try {
      const parsed = JSON.parse(task.stationIds)
      if (Array.isArray(parsed) && parsed.length > 0) {
        stationIds = parsed
      }
    } catch (e) {
    }
  }
  stationName = stationIds.map(sid => stationMap.value[sid]?.name || `台站-${sid}`).join('、')
  const itemLabel = task.status === '待复检' ? '（待复检 — 上次检查不合格）' : '（尚未录入检查明细）'
  return {
    id: `virtual-${task.id}`,
    virtual: true,
    taskId: task.id,
    stationId: task.stationId,
    taskName: task.name,
    taskStatus: task.status || '',
    stationName,
    stationIdList: stationIds,
    inspectionMode: task.inspectionMode || '',
    itemName: itemLabel,
    result: '-',
    inspector: resolveInspectorNames(task.assignee) || '-',
    checkedAt: '',
    evidenceUrls: ''
  }
}

const matchKeyword = (row, keyword) => {
  if (!keyword) return true
  const source = [row.taskName, row.itemName, row.inspector, row.stationName].join('|')
  return String(source).includes(keyword)
}

const getRowClassName = ({ row }) => {
  if (row.taskId) {
    const newTaskIds = JSON.parse(sessionStorage.getItem('new-task-ids') || '[]')
    if (newTaskIds.includes(row.taskId)) {
      return 'new-task-row'
    }
  }
  return ''
}

const clearNewTaskHighlight = () => {
  setTimeout(() => {
    sessionStorage.removeItem('new-task-ids')
    if (typeof window.clearInspectionBadge === 'function') {
      window.clearInspectionBadge()
    }
    tableData.value = [...tableData.value]
  }, 10000)
}
const getNewTaskCountForCategory = (category) => {
  const newTaskIds = JSON.parse(sessionStorage.getItem('new-task-ids') || '[]')
  if (!newTaskIds.length) return 0
  let count = 0
  for (const tid of newTaskIds) {
    const task = taskMap.value[tid]
    if (task && task.taskType === category.taskType && task.checkCategory === category.value) {
      count++
    }
  }
  return count
}

const load = async () => {
  const meta = getCurrentMeta()
  const inspectionParams = {
    ...query,
    page: 1,
    size: 1000,
    objectType: meta.objectType,
    taskType: meta.taskType,
    checkCategory: meta.value
  }
  const taskParams = {
    page: 1,
    size: 1000,
    taskType: meta.taskType,
    checkCategory: meta.value
  }

  const [inspectionRes, taskRes] = await Promise.all([
    fetchInspections(inspectionParams),
    fetchTasks(taskParams)
  ])
  if (!inspectionRes.success) return

  if (taskRes?.success) {
    const records = Array.isArray(taskRes.data) ? taskRes.data : taskRes.data?.records || []
    taskOptions.value = records || []
    const map = {}
    ;(records || []).forEach((item) => {
      map[item.id] = item
    })
    taskMap.value = map
  }

  const taskRowsRaw = Array.isArray(taskRes.data) ? taskRes.data : taskRes.data?.records || []
  const allowedTaskRows = taskRowsRaw.filter((task) => !['待审核', '已取消'].includes(task.status))
  const recordRows = (Array.isArray(inspectionRes.data) ? inspectionRes.data : inspectionRes.data.records || [])
    .filter((item) => {
      const status = taskMap.value[item.taskId]?.status
      return !status || !['待审核', '已取消'].includes(status)
    })
    .map(normalizeRow)
  const recordMap = new Map()
  recordRows.forEach(row => {
    const key = `${row.taskId}_${row.stationId}`
    const existing = recordMap.get(key)
    if (!existing || (row.round || 1) > (existing.round || 1)) {
      recordMap.set(key, row)
    }
  })
  const dedupedRecords = Array.from(recordMap.values())
  const recordedTaskIds = new Set(dedupedRecords.map((item) => item.taskId).filter(Boolean))
  const virtualRows = allowedTaskRows.filter((task) => !recordedTaskIds.has(task.id)).map(buildVirtualRow)

  const merged = [...dedupedRecords, ...virtualRows].filter((row) => {
    if (!matchKeyword(row, query.keyword)) return false
    if (query.result && row.result !== query.result) return false
    return true
  })

  const start = (query.page - 1) * query.size
  const end = start + query.size
  tableData.value = merged.slice(start, end)
  total.value = merged.length
}

const onCategoryChange = () => {
  query.page = 1
  load()
}

const onSearch = () => {
  query.page = 1
  load()
}

const onReset = () => {
  Object.assign(query, { page: 1, size: 10, keyword: '', result: '' })
  load()
}


const syncTaskMeta = (taskId) => {
  const task = taskMap.value[taskId]
  if (!task) return
  form.inspectionMode = task.inspectionMode || form.inspectionMode
  form.stationId = task.stationId || form.stationId
  form.taskName = task.name || form.taskName
  form.stationName = stationMap.value[task.stationId]?.name || form.stationName
  if (task.assignee) {
    form.inspectorName = resolveInspectorDisplay(task.assignee)
    form.inspectorDisplay = resolveInspectorNames(task.assignee)
  }
}

const loadFrequencyForStation = async (stationId) => {
  if (!stationId) {
    currentFrequency.value = null
    return
  }
  if (frequencyCache.value[stationId]) {
    currentFrequency.value = frequencyCache.value[stationId]
    refreshDetailResults()
    return
  }
  const res = await fetchFrequencies({ page: 1, size: 1, stationId })
  if (res.success) {
    const records = Array.isArray(res.data) ? res.data : res.data.records
    const record = records && records.length ? records[0] : null
    frequencyCache.value[stationId] = record
    currentFrequency.value = record
    refreshDetailResults()
  }
}

const applyDetailData = (data) => {
  if (!data) return
  Object.entries(data).forEach(([key, value]) => {
    form[key] = value
  })
  if (form.isQualified !== true && form.isQualified !== false) {
    const raw = form.isQualified
    form.isQualified = raw === true || raw === 1 || raw === '1' || raw === 'true' || raw === '合格'
  }
}

const loadDetail = async (taskId, stationId) => {
  const meta = getCurrentMeta()
  if (!taskId || !meta.detailKey) return
  const sid = stationId || form.stationId
  const res = await fetchInspectionDetail(meta.detailKey, { taskId, stationId: sid })
  if (res.success) {
    applyDetailData(res.data)
    setReinspectionLocks(sid, res.data)
    refreshDetailResults()
  }
  return res.success ? res.data : null
}

const openDialog = async (row) => {
  resetDetailFields()
  Object.assign(form, {
    taskId: null,
    stationId: null,
    stationIdList: [],
    taskName: '',
    stationName: '',
    inspectionMode: '日常检查',
    inspectorName: '',
    inspectorDisplay: '',
    checkedAt: '',
    isQualified: true,
    noQualifiedReason: '',
    rectificationDeadline: '',
    processRecord: '',
    evidenceUrls: '',
    remarks: ''
  })
  multiStationCache.value = {}
  activeStationTab.value = ''
  reinspectionMode.value = false
  reinspectionLocks.value = {}

  if (row) {
    form.taskId = row.taskId || null
    form.stationId = row.stationId || null
    form.inspectionMode = row.inspectionMode || form.inspectionMode
    form.inspectorName = resolveInspectorNames(row.inspector)
    form.inspectorDisplay = resolveInspectorNames(row.inspector)
    form.checkedAt = formatDateOnly(row.checkedAt)
    form.processRecord = row.processRecord || ''
    form.evidenceUrls = row.evidenceUrls || ''
    form.remarks = row.remarks || ''
  }

  visible.value = true
  if (form.taskId) {
    syncTaskMeta(form.taskId)
    await loadMultiStationIds(form.taskId)
    await loadFrequencyForStation(form.stationId)
    await loadDetailForCurrentStation(form.taskId)
  }
}

const openDialogForTask = async (taskId) => {
  await openDialog()
  form.taskId = taskId
  syncTaskMeta(taskId)
  await loadMultiStationIds(taskId)
  await loadFrequencyForStation(form.stationId)
  await loadDetailForCurrentStation(taskId)
}

const openReinspection = async (row) => {
  resetDetailFields()
  Object.assign(form, {
    taskId: row.taskId || null,
    stationId: row.stationId || null,
    stationIdList: [],
    taskName: row.taskName || '',
    stationName: row.stationName || '',
    inspectionMode: row.inspectionMode || '日常检查',
    inspectorName: resolveInspectorNames(row.inspector),
    inspectorDisplay: resolveInspectorNames(row.inspector),
    checkedAt: '',
    isQualified: true,
    noQualifiedReason: '',
    rectificationDeadline: '',
    processRecord: '',
    evidenceUrls: '',
    remarks: ''
  })
  multiStationCache.value = {}
  activeStationTab.value = ''
  reinspectionMode.value = true
  reinspectionLocks.value = {}
  visible.value = true
  if (form.taskId) {
    syncTaskMeta(form.taskId)
    await loadMultiStationIds(form.taskId)
    // 仅筛选不合格的台站用于复检
    if (form.stationIdList.length > 1) {
      const failedStationIds = []
      const detailKey = getCurrentMeta().detailKey
      for (const sid of form.stationIdList) {
        try {
          const res = await fetchInspectionDetail(detailKey, { taskId: form.taskId, stationId: sid })
          if (res.success && res.data && res.data.isQualified !== true) {
            failedStationIds.push(sid)
          }
        } catch (e) {
          failedStationIds.push(sid)
        }
      }
      if (failedStationIds.length === 0) {
        ElMessage.warning('所有台站均已合格，无需复检')
        visible.value = false
        return
      }
      form.stationIdList = failedStationIds
      form.stationName = failedStationIds.map(sid => stationMap.value[sid]?.name || `台站-${sid}`).join('、')
      form.stationId = failedStationIds[0]
      activeStationTab.value = String(failedStationIds[0])
    }
    await loadFrequencyForStation(form.stationId)
    await loadDetailForCurrentStation(form.taskId)
  }
}

const loadMultiStationIds = async (taskId) => {
  const task = taskMap.value[taskId]
  if (!task) return
  let stationIds = []
  if (task.stationIds) {
    try {
      stationIds = JSON.parse(task.stationIds)
    } catch (e) {
      stationIds = []
    }
  }
  if (!stationIds.length && task.stationId) {
    stationIds = [task.stationId]
  }
  form.stationIdList = stationIds
  form.stationName = stationIds.map(sid => stationMap.value[sid]?.name || `台站-${sid}`).join('、')
  if (stationIds.length > 1) {
    activeStationTab.value = String(stationIds[0])
    form.stationId = stationIds[0]
  }
}

const loadDetailForCurrentStation = async (taskId) => {
  const sid = form.stationIdList.length > 1 ? Number(activeStationTab.value) : form.stationId
  form.stationId = sid
  await loadFrequencyForStation(sid)
  await loadDetail(taskId)
}

const save = async () => {
  // 多台站任务时，保存所有台站
  const stationsToSave = form.stationIdList.length > 1 ? form.stationIdList : [form.stationId]

  // 保存前缓存当前标签页数据
  if (form.stationIdList.length > 1 && form.stationId) {
    const cacheData = {}
    allDetailKeys.forEach((key) => { cacheData[key] = form[key] })
    allResultKeys.forEach((key) => { cacheData[key] = form[key] })
    multiStationCache.value[form.stationId] = cacheData
  }

  // 第一轮：校验所有台站，不修改状态
  for (const sid of stationsToSave) {
    const data = form.stationIdList.length > 1 && multiStationCache.value[sid]
      ? multiStationCache.value[sid]
      : form

    if (!form.taskId || !sid || !form.inspectorName || !form.checkedAt || form.isQualified === null || form.isQualified === undefined) {
      ElMessage.warning('请完整填写检查关键信息')
      return
    }
    if (form.isQualified === false && !form.rectificationDeadline) {
      ElMessage.warning('不合格记录需填写整改期限')
      return
    }

    // 基于字段类型做基础校验
    for (const field of detailFields.value) {
      const meta = getFieldMeta(field)
      const val = data[field.key]
      if (['number', 'money', 'percent'].includes(meta.type) && val !== null && val !== undefined && val !== '') {
        if (Number.isNaN(Number(val))) {
          ElMessage.warning(`${field.label} 需填写数值类型 (${meta.unit || ''})`)
          return
        }
      }
    }
  }

  // 第二轮：所有校验通过后保存
  for (const sid of stationsToSave) {
    if (form.stationIdList.length > 1) {
      form.stationId = sid
      if (multiStationCache.value[sid]) {
        Object.entries(multiStationCache.value[sid]).forEach(([key, value]) => {
          form[key] = value
        })
      } else {
        resetDetailFields()
      }
    }

    const payload = {
      taskId: form.taskId,
      stationId: form.stationId,
      inspectorName: form.inspectorName,
      checkedAt: form.checkedAt,
      isQualified: form.isQualified,
      noQualifiedReason: form.noQualifiedReason,
      rectificationDeadline: form.rectificationDeadline || null,
      processRecord: form.processRecord,
      evidenceUrls: form.evidenceUrls,
      remarks: form.remarks
    }
    detailFields.value.forEach((field) => {
      payload[field.key] = form[field.key]
      const resultKey = getResultKey(field)
      const autoResult = autoJudge(field.key)
      payload[resultKey] = form[resultKey] || autoResult || ''
    })

    await saveInspectionDetail(getCurrentMeta().detailKey, payload)
  }

  ElMessage.success('保存成功')
  visible.value = false
  load()
}

const deleteRecord = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除任务「${row.taskName}」的检查记录吗？删除后将恢复为待录入状态。`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteInspection(row.id)
    ElMessage.success('已删除检查记录')
    load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const uploadEvidence = async (fileObj) => {
  const res = await uploadFile(fileObj.raw, 'inspection-evidence')
  if (res.success) {
    const url = res.data?.url || ''
    form.evidenceUrls = form.evidenceUrls ? `${form.evidenceUrls},${url}` : url
    ElMessage.success('上传成功')
  }
}

const updateDetailResult = (fieldKey) => {
  const resultKey = `${fieldKey}_result`
  const autoResult = autoJudge(fieldKey)
  form[resultKey] = autoResult
}

const refreshDetailResults = () => {
  detailFields.value.forEach((field) => {
    updateDetailResult(field.key)
  })
}

watch(
  () => activeStationTab.value,
  async (newTab, oldTab) => {
    if (!newTab || form.stationIdList.length <= 1) return
    const newSid = Number(newTab)
    // 缓存当前台站数据
    if (oldTab) {
      const oldSid = Number(oldTab)
      const cacheData = {}
      allDetailKeys.forEach((key) => {
        cacheData[key] = form[key]
      })
      allResultKeys.forEach((key) => {
        cacheData[key] = form[key]
      })
      multiStationCache.value[oldSid] = cacheData
    }
    // 切换到新台站
    form.stationId = newSid
    if (multiStationCache.value[newSid]) {
      Object.entries(multiStationCache.value[newSid]).forEach(([key, value]) => {
        form[key] = value
      })
      setReinspectionLocks(newSid, multiStationCache.value[newSid])
    } else {
      resetDetailFields()
      // 从后端加载已有数据
      if (form.taskId) {
        const res = await fetchInspectionDetail(getCurrentMeta().detailKey, { taskId: form.taskId, stationId: newSid })
        if (res.success && res.data) {
          applyDetailData(res.data)
          setReinspectionLocks(newSid, res.data)
        }
      }
    }
    refreshDetailResults()
    await loadFrequencyForStation(newSid)
  }
)

watch(
  () => resultActiveStation.value,
  async (newTab) => {
    if (!newTab || resultStationIdList.value.length <= 1) return
    const newSid = Number(newTab)
    resultStation.value = stationMap.value[newSid] || null
    if (resultDetailCache.value[newSid]) {
      resultDetailData.value = resultDetailCache.value[newSid]
    } else {
      await loadResultDetail(resultTaskInfo.value.taskId, newSid)
    }
  }
)

const downloadRecords = async () => {
  const blob = await exportInspections()
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'inspection-records.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

const downloadResultPdf = async () => {
  if (!resultPdfRef.value) {
    ElMessage.warning('未找到检查结果内容')
    return
  }
  downloadingPdf.value = true
  isPdfExporting.value = true
  resultPdfRef.value.classList.add('pdf-exporting')
  try {
    await nextTick()
    const canvas = await html2canvas(resultPdfRef.value, {
      scale: Math.max(1.5, window.devicePixelRatio || 1),
      useCORS: true,
      backgroundColor: '#ffffff'
    })
    const imgData = canvas.toDataURL('image/jpeg', 0.82)
    const pdf = new jsPDF({ orientation: 'p', unit: 'mm', format: 'a4', compress: true })
    const pageWidth = pdf.internal.pageSize.getWidth()
    const pageHeight = pdf.internal.pageSize.getHeight()
    const imgWidth = pageWidth
    const imgHeight = (canvas.height * imgWidth) / canvas.width

    let heightLeft = imgHeight
    let position = 0

    pdf.addImage(imgData, 'JPEG', 0, position, imgWidth, imgHeight)
    heightLeft -= pageHeight

    while (heightLeft > 0) {
      position -= pageHeight
      pdf.addPage()
      pdf.addImage(imgData, 'JPEG', 0, position, imgWidth, imgHeight)
      heightLeft -= pageHeight
    }

    const name = String(resultTaskInfo.value?.name || 'inspection-result')
    const safeName = name.replace(/[\\/:*?"<>|]/g, '_')
    const date = formatDateOnly(resultTaskInfo.value?.checkedAt)
    const fileName = date ? `${safeName}-${date}.pdf` : `${safeName}.pdf`
    pdf.save(fileName)
  } catch (error) {
    ElMessage.error('导出PDF失败，请重试')
  } finally {
    resultPdfRef.value?.classList.remove('pdf-exporting')
    isPdfExporting.value = false
    downloadingPdf.value = false
  }
}

onMounted(async () => {
  if (typeof window.clearInspectionBadge === 'function') {
    window.clearInspectionBadge()
  }
  await loadBaseOptions()
  await load()
  clearNewTaskHighlight()
})

watch(
  () => form.stationId,
  async (value) => {
    if (!value) return
    await loadFrequencyForStation(value)
  }
)

const normalizeText = (value) => String(value || '').trim().toLowerCase()

const parseDateOnly = (value) => {
  if (!value) return null
  if (Array.isArray(value)) {
    const [year, month, day] = value
    if (!year || !month || !day) return null
    return new Date(`${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`)
  }
  const raw = String(value).replace('T', ' ').trim()
  const parts = raw.split(/[-/:,\s]/).filter(Boolean)
  if (parts.length >= 3 && parts[0].length === 4) {
    return new Date(`${parts[0]}-${String(parts[1]).padStart(2, '0')}-${String(parts[2]).padStart(2, '0')}`)
  }
  return null
}

const isTruthyText = (value) => /已|是|有|合格|存在|报送/.test(String(value || ''))
const isFalsyText = (value) => /未|否|无|不合格|不存在/.test(String(value || ''))

const parseFrequencyRange = (raw) => {
  if (!raw) return null
  const text = String(raw).trim().toLowerCase()
  // 提取单位：GHz、MHz 或 kHz（默认 MHz）
  let defaultUnit = 'mhz'
  if (text.includes('ghz')) defaultUnit = 'ghz'
  else if (text.includes('khz')) defaultUnit = 'khz'
  // 去除单位以便解析
  const cleaned = text.replace(/ghz|mhz|khz/g, '').trim()
  // 匹配范围：数值 - 数值
  const rangeMatch = cleaned.match(/^([\d.]+)\s*[-–—to至~]\s*([\d.]+)$/)
  if (rangeMatch) {
    const start = parseFloat(rangeMatch[1])
    const end = parseFloat(rangeMatch[2])
    if (Number.isNaN(start) || Number.isNaN(end)) return null
    // 转换为 MHz 以便统一比较
    const multiplier = defaultUnit === 'ghz' ? 1000 : defaultUnit === 'khz' ? 0.001 : 1
    return { startMHz: start * multiplier, endMHz: end * multiplier }
  }
  // 匹配单个数值
  const singleMatch = cleaned.match(/^([\d.]+)$/)
  if (singleMatch) {
    const value = parseFloat(singleMatch[1])
    if (Number.isNaN(value)) return null
    const multiplier = defaultUnit === 'ghz' ? 1000 : defaultUnit === 'khz' ? 0.001 : 1
    const mhz = value * multiplier
    return { startMHz: mhz, endMHz: mhz }
  }
  return null
}

const compareFrequencyContainment = (inputRaw, expectedRaw) => {
  const inputParsed = parseFrequencyRange(inputRaw)
  const expectedParsed = parseFrequencyRange(expectedRaw)
  if (!inputParsed || !expectedParsed) {
    // 回退到子串匹配，并检查最小长度
    const input = normalizeText(inputRaw)
    const expected = normalizeText(expectedRaw)
    if (input.length < 3 || expected.length < 3) return null
    // 实际值必须完全包含在许可值范围内
    return expected.includes(input) ? '合格' : '不合格'
  }
  // 输入范围必须完全包含在许可范围内
  return (inputParsed.startMHz >= expectedParsed.startMHz && inputParsed.endMHz <= expectedParsed.endMHz) ? '合格' : '不合格'
}

const compareRegionContainment = (inputRaw, expectedRaw) => {
  const input = String(inputRaw || '').trim()
  const expected = String(expectedRaw || '').trim()
  if (!input || !expected) return null
  // 最小长度：有意义的地理名称至少 2 个字符（如"北京"、"成都"）
  if (input.length < 2) return null
  // 精确匹配
  if (input === expected) return '合格'
  // 输入是更具体的子区域（期望值的后缀）："青羊区" ⊆ "成都市青羊区"
  if (expected.endsWith(input)) return '合格'
  // 输入比期望更宽泛（期望值是输入的后缀）："成都市" ⊇ "成都市青羊区" → 不合格
  if (input.endsWith(expected)) return '不合格'
  // 期望值包含输入 → 实际地域在许可范围内 → 合格
  if (expected.includes(input)) return '合格'
  // 输入包含期望值 → 实际地域超出许可范围 → 不合格
  if (input.includes(expected)) return '不合格'
  return '不合格'
}

const customSelectOptions = {
  // === 地面频率使用（地面频率）===
  check_1_1_1_6_1: [
    { label: '发射设备型号核准通过', value: '发射设备型号核准通过' },
    { label: '发射设备型号核准未通过', value: '发射设备型号核准未通过' }
  ],
  check_1_1_1_6_2: [
    { label: '边境协调协议已执行', value: '边境协调协议已执行' },
    { label: '边境协调协议未执行', value: '边境协调协议未执行' }
  ],
  check_1_1_2: [
    { label: '技术方案与承诺条件未改变', value: '技术方案与承诺条件未改变' },
    { label: '技术方案与承诺条件已改变', value: '技术方案与承诺条件已改变' }
  ],
  check_1_1_3: [
    { label: '专业技术人员配置符合要求', value: '专业技术人员配置符合要求' },
    { label: '专业技术人员配置不符合要求', value: '专业技术人员配置不符合要求' }
  ],
  check_1_1_4: [
    { label: '未产生有害干扰', value: '未产生有害干扰' },
    { label: '已产生有害干扰', value: '已产生有害干扰' }
  ],
  check_1_1_6: [
    { label: '许可后2年内已投入使用', value: '许可后2年内已投入使用' },
    { label: '许可后2年内未投入使用', value: '许可后2年内未投入使用' }
  ],
  check_1_1_5: [
    { label: '未发现伪造/冒用许可行为', value: '未发现伪造/冒用许可行为' },
    { label: '发现伪造/冒用许可行为', value: '发现伪造/冒用许可行为' }
  ],
  check_1_1_8: [
    { label: '年度报告已按要求报送', value: '年度报告已按要求报送' },
    { label: '年度报告未按要求报送', value: '年度报告未按要求报送' }
  ],

  // === 卫星频率使用（卫星频率）===
  check_1_2_1_6_1: [
    { label: '卫星网络资料已申报', value: '卫星网络资料已申报' },
    { label: '卫星网络资料未申报', value: '卫星网络资料未申报' }
  ],
  check_1_2_1_6_2: [
    { label: '国际国内协调程序已履行', value: '国际国内协调程序已履行' },
    { label: '国际国内协调程序未履行', value: '国际国内协调程序未履行' }
  ],
  check_1_2_1_6_4: [
    { label: '符合RR21.16款规定', value: '符合RR21.16款规定' },
    { label: '不符合RR21.16款规定', value: '不符合RR21.16款规定' }
  ],
  check_1_2_2: [
    { label: '技术方案与承诺条件未改变', value: '技术方案与承诺条件未改变' },
    { label: '技术方案与承诺条件已改变', value: '技术方案与承诺条件已改变' }
  ],
  check_1_2_3: [
    { label: '专业技术人员配置符合要求', value: '专业技术人员配置符合要求' },
    { label: '专业技术人员配置不符合要求', value: '专业技术人员配置不符合要求' }
  ],
  check_1_2_4: [
    { label: '未产生有害干扰', value: '未产生有害干扰' },
    { label: '已产生有害干扰', value: '已产生有害干扰' }
  ],
  check_1_2_6: [
    { label: '许可后2年内已投入使用', value: '许可后2年内已投入使用' },
    { label: '许可后2年内未投入使用', value: '许可后2年内未投入使用' }
  ],
  check_1_2_5: [
    { label: '未发现伪造/冒用许可行为', value: '未发现伪造/冒用许可行为' },
    { label: '发现伪造/冒用许可行为', value: '发现伪造/冒用许可行为' }
  ],
  check_1_2_8: [
    { label: '年度报告已按要求报送', value: '年度报告已按要求报送' },
    { label: '年度报告未按要求报送', value: '年度报告未按要求报送' }
  ],

  // === 卫星通信网频率（卫星通信网）===
  check_1_3_1_6_3: [
    { label: '终端地球站已按要求部署', value: '终端地球站已按要求部署' },
    { label: '终端地球站未按要求部署', value: '终端地球站未按要求部署' }
  ],
  check_1_3_1_6_4: [
    { label: '网内地球站执照已办理', value: '网内地球站执照已办理' },
    { label: '网内地球站执照未办理', value: '网内地球站执照未办理' }
  ],
  check_1_3_2: [
    { label: '技术方案与承诺条件未改变', value: '技术方案与承诺条件未改变' },
    { label: '技术方案与承诺条件已改变', value: '技术方案与承诺条件已改变' }
  ],
  check_1_3_3: [
    { label: '专业技术人员配置符合要求', value: '专业技术人员配置符合要求' },
    { label: '专业技术人员配置不符合要求', value: '专业技术人员配置不符合要求' }
  ],
  check_1_3_4: [
    { label: '未产生有害干扰', value: '未产生有害干扰' },
    { label: '已产生有害干扰', value: '已产生有害干扰' }
  ],
  check_1_3_6: [
    { label: '许可后2年内已投入使用', value: '许可后2年内已投入使用' },
    { label: '许可后2年内未投入使用', value: '许可后2年内未投入使用' }
  ],
  check_1_3_5: [
    { label: '未发现伪造/冒用许可行为', value: '未发现伪造/冒用许可行为' },
    { label: '发现伪造/冒用许可行为', value: '发现伪造/冒用许可行为' }
  ],
  check_1_3_8: [
    { label: '年度报告已按要求报送', value: '年度报告已按要求报送' },
    { label: '年度报告未按要求报送', value: '年度报告未按要求报送' }
  ],

  // === 地面台站（地面台站）===
  check_2_1_1_4_1: [
    { label: '发射设备型号核准代码一致', value: '发射设备型号核准代码一致' },
    { label: '发射设备型号核准代码不一致', value: '发射设备型号核准代码不一致' }
  ],
  check_2_1_1_5_1: [
    { label: '业余无线电台操作证书符合要求', value: '业余无线电台操作证书符合要求' },
    { label: '业余无线电台操作证书不符合要求', value: '业余无线电台操作证书不符合要求' }
  ],
  check_2_1_1_5_2: [
    { label: '已向ITU申报且资料一致', value: '已向ITU申报且资料一致' },
    { label: '未向ITU申报或资料不一致', value: '未向ITU申报或资料不一致' }
  ],
  check_2_1_1_5_3: [
    { label: '边境频率协调程序已履行', value: '边境频率协调程序已履行' },
    { label: '边境频率协调程序未履行', value: '边境频率协调程序未履行' }
  ],
  check_2_1_1_5_4: [
    { label: '符合3000-5000MHz协调管理办法', value: '符合3000-5000MHz协调管理办法' },
    { label: '不符合3000-5000MHz协调管理办法', value: '不符合3000-5000MHz协调管理办法' }
  ],
  check_2_1_2: [
    { label: '技术方案与承诺条件未改变', value: '技术方案与承诺条件未改变' },
    { label: '技术方案与承诺条件已改变', value: '技术方案与承诺条件已改变' }
  ],
  check_2_1_3: [
    { label: '人员配置符合要求', value: '人员配置符合要求' },
    { label: '人员配置不符合要求', value: '人员配置不符合要求' }
  ],
  check_2_1_4: [
    { label: '未产生有害干扰', value: '未产生有害干扰' },
    { label: '已产生有害干扰', value: '已产生有害干扰' }
  ],
  check_2_1_5: [
    { label: '未发现许可事项外信号', value: '未发现许可事项外信号' },
    { label: '发现许可事项外信号', value: '发现许可事项外信号' }
  ],
  check_2_1_6: [
    { label: '识别码/呼号使用合规', value: '识别码/呼号使用合规' },
    { label: '识别码/呼号使用违规', value: '识别码/呼号使用违规' }
  ],
  check_2_1_7: [
    { label: '已按规定进行定期维护', value: '已按规定进行定期维护' },
    { label: '未按规定进行定期维护', value: '未按规定进行定期维护' }
  ],
  check_2_1_8: [
    { label: '通联日志记录完整', value: '通联日志记录完整' },
    { label: '通联日志记录不完整', value: '通联日志记录不完整' }
  ],

  // === 空间电台（空间电台）===
  check_2_2_1_2_1: [
    { label: '民用', value: '民用' },
    { label: '商用', value: '商用' },
    { label: '军用', value: '军用' },
    { label: '科研', value: '科研' }
  ],
  check_2_2_1_2_2: [
    { label: '通信', value: '通信' },
    { label: '导航', value: '导航' },
    { label: '遥感', value: '遥感' },
    { label: '广播', value: '广播' },
    { label: '其他', value: '其他' }
  ],
  check_2_2_1_4_1: [
    { label: '已向ITU申报且资料一致', value: '已向ITU申报且资料一致' },
    { label: '未向ITU申报或资料不一致', value: '未向ITU申报或资料不一致' }
  ],
  check_2_2_1_4_2: [
    { label: '国内国际协调程序已履行', value: '国内国际协调程序已履行' },
    { label: '国内国际协调程序未履行', value: '国内国际协调程序未履行' }
  ],
  check_2_2_1_4_3: [
    { label: '已采取有效干扰规避措施', value: '已采取有效干扰规避措施' },
    { label: '未采取有效干扰规避措施', value: '未采取有效干扰规避措施' }
  ],
  check_2_2_2: [
    { label: '技术方案与承诺条件未改变', value: '技术方案与承诺条件未改变' },
    { label: '技术方案与承诺条件已改变', value: '技术方案与承诺条件已改变' }
  ],
  check_2_2_3: [
    { label: '人员配置符合要求', value: '人员配置符合要求' },
    { label: '人员配置不符合要求', value: '人员配置不符合要求' }
  ],
  check_2_2_4: [
    { label: '未产生有害干扰', value: '未产生有害干扰' },
    { label: '已产生有害干扰', value: '已产生有害干扰' }
  ],
  check_2_2_5: [
    { label: '未发现许可事项外信号', value: '未发现许可事项外信号' },
    { label: '发现许可事项外信号', value: '发现许可事项外信号' }
  ],

  // === 卫星地球站（卫星地球站）===
  check_2_3_1_2_2: [
    { label: '民用', value: '民用' },
    { label: '商用', value: '商用' },
    { label: '军用', value: '军用' },
    { label: '科研', value: '科研' }
  ],
  check_2_3_1_2_5: [
    { label: '发射设备型号与执照一致', value: '发射设备型号与执照一致' },
    { label: '发射设备型号与执照不一致', value: '发射设备型号与执照不一致' }
  ],
  check_2_3_1_4_1: [
    { label: '已向ITU申报且资料一致', value: '已向ITU申报且资料一致' },
    { label: '未向ITU申报或资料不一致', value: '未向ITU申报或资料不一致' }
  ],
  check_2_3_1_4_2: [
    { label: '国际协调程序已履行', value: '国际协调程序已履行' },
    { label: '国际协调程序未履行', value: '国际协调程序未履行' }
  ],
  check_2_3_1_4_3: [
    { label: '动中通指标限值符合规定', value: '动中通指标限值符合规定' },
    { label: '动中通指标限值不符合规定', value: '动中通指标限值不符合规定' }
  ],
  check_2_3_1_4_4: [
    { label: '符合3000-5000MHz协调管理办法', value: '符合3000-5000MHz协调管理办法' },
    { label: '不符合3000-5000MHz协调管理办法', value: '不符合3000-5000MHz协调管理办法' }
  ],
  check_2_3_2: [
    { label: '技术方案与承诺条件未改变', value: '技术方案与承诺条件未改变' },
    { label: '技术方案与承诺条件已改变', value: '技术方案与承诺条件已改变' }
  ],
  check_2_3_3: [
    { label: '人员配置符合要求', value: '人员配置符合要求' },
    { label: '人员配置不符合要求', value: '人员配置不符合要求' }
  ],
  check_2_3_4: [
    { label: '未产生有害干扰', value: '未产生有害干扰' },
    { label: '已产生有害干扰', value: '已产生有害干扰' }
  ],
  check_2_3_5: [
    { label: '未发现许可事项外信号', value: '未发现许可事项外信号' },
    { label: '发现许可事项外信号', value: '发现许可事项外信号' }
  ],
  check_2_3_7: [
    { label: '已按规定进行定期维护', value: '已按规定进行定期维护' },
    { label: '未按规定进行定期维护', value: '未按规定进行定期维护' }
  ]
}

const frequencyFieldMap = new Map([
  ['check_1_1_1_1', { field: 'frequencyRange', type: 'freq-range' }],
  ['check_1_2_1_1', { field: 'frequencyRange', type: 'freq-range' }],
  ['check_1_3_1_1', { field: 'frequencyRange', type: 'freq-range' }],
  ['check_1_1_1_2', { field: 'usageRegion', type: 'region' }],
  ['check_1_2_1_2', { field: 'usageRegion', type: 'region' }],
  ['check_1_3_1_2', { field: 'usageRegion', type: 'region' }],
  ['check_1_1_1_3', { field: 'businessUsage', type: 'text' }],
  ['check_1_2_1_3', { field: 'businessUsage', type: 'text' }],
  ['check_1_3_1_3', { field: 'businessUsage', type: 'text' }],
  ['check_1_1_1_4', { field: 'usageDeadline', type: 'date' }],
  ['check_1_2_1_4', { field: 'usageDeadline', type: 'date' }],
  ['check_1_3_1_4', { field: 'usageDeadline', type: 'date' }],
  ['check_1_1_1_5', { field: 'usageRate', type: 'min-number' }],
  ['check_1_2_1_5', { field: 'usageRate', type: 'min-number' }],
  ['check_1_3_1_5', { field: 'usageRate', type: 'min-number' }],
  ['check_1_1_7', { field: 'spectrumFeePaid', type: 'amount' }],
  ['check_1_2_7', { field: 'spectrumFeePaid', type: 'amount' }],
  ['check_1_3_7', { field: 'spectrumFeePaid', type: 'amount' }],
  ['check_1_1_8', { field: 'annualReportSubmitted', type: 'boolean' }],
  ['check_1_2_8', { field: 'annualReportSubmitted', type: 'boolean' }],
  ['check_1_3_8', { field: 'annualReportSubmitted', type: 'boolean' }],
  ['check_2_1_1_3_1', { field: 'frequencyRange', type: 'freq-range' }],
  ['check_2_2_1_3_1', { field: 'frequencyRange', type: 'freq-range' }],
  ['check_2_3_1_3_1', { field: 'frequencyRange', type: 'freq-range' }],
  ['check_2_1_9', { field: 'spectrumFeePaid', type: 'amount' }],
  ['check_2_2_6', { field: 'spectrumFeePaid', type: 'amount' }],
  ['check_2_3_6', { field: 'spectrumFeePaid', type: 'amount' }],
  ['check_1_2_1_6_3', { field: 'eirpSpectralDensity', type: 'number' }],
  ['check_1_3_1_6_1', { field: 'antennaSize', type: 'number' }],
  ['check_1_3_1_6_2', { field: 'eirpSpectralDensity', type: 'number' }]
])

const stationFieldMap = new Map([
  ['check_2_1_1_2_1', { field: 'stationCode', type: 'text' }],
  ['check_2_1_1_2_2', { field: 'address', type: 'text' }],
  ['check_2_1_1_2_3', { field: 'coordinates', type: 'coordinates' }],
  // check_2_2_1_2_2 (业务类别) 无对应数据库字段，需人工判定——通信/导航/遥感等与serviceType语义不同
  ['check_2_3_1_2_1', { field: 'stationType', type: 'text' }],
  ['check_2_3_1_2_3', { field: 'address', type: 'text' }],
  ['check_2_3_1_2_4', { field: 'coordinates', type: 'coordinates' }],
  ['check_2_1_1_3_2', { field: 'transmitPower', type: 'number' }],
  ['check_2_1_1_3_3', { field: 'occupiedBandwidth', type: 'number' }],
  ['check_2_1_1_4_2', { field: 'antennaGain', type: 'number' }],
  ['check_2_1_1_4_3', { field: 'polarization', type: 'text' }],
  ['check_2_1_1_4_4', { field: 'antennaHeight', type: 'number' }],
  ['check_2_2_1_2_3', { field: 'orbitalPosition', type: 'text' }],
  ['check_2_2_1_3_2', { field: 'polarization', type: 'text' }],
  ['check_2_2_1_3_3', { field: 'occupiedBandwidth', type: 'number' }],
  ['check_2_2_1_3_4', { field: 'maxEirp', type: 'number' }],
  ['check_2_3_1_2_6', { field: 'antennaGain', type: 'number' }],
  ['check_2_3_1_2_7', { field: 'antennaSize', type: 'number' }],
  ['check_2_3_1_2_8', { field: 'antennaHeight', type: 'number' }],
  ['check_2_3_1_3_2', { field: 'polarization', type: 'text' }],
  ['check_2_3_1_3_3', { field: 'occupiedBandwidth', type: 'number' }],
  ['check_2_3_1_3_4', { field: 'transmitPower', type: 'number' }],
  ['check_2_3_1_3_5', { field: 'totalBandwidth', type: 'number' }]
])

const autoJudge = (fieldKey) => {
  const value = form[fieldKey]
  if (value === null || value === undefined || value === '') return ''

  if (
    fieldKey === 'check_1_1_5' ||
    fieldKey === 'check_1_2_5' ||
    fieldKey === 'check_1_3_5'
  ) {
    const v = String(value).trim()
    if (v === '否') return '合格'
    if (v === '是') return '不合格'
    if (v === '无') return '合格'
    if (v === '有') return '不合格'
  }

  if (
    fieldKey === 'check_1_1_8' ||
    fieldKey === 'check_1_2_8' ||
    fieldKey === 'check_1_3_8'
  ) {
    const txt = String(value).trim()
    if (txt.includes('频率使用报告') || txt.includes('台站使用报告')) {
      return '合格'
    }
  }

  if (frequencyFieldMap.has(fieldKey)) {
    const meta = frequencyFieldMap.get(fieldKey)
    const freq = currentFrequency.value
    if (!freq || freq[meta.field] === null || freq[meta.field] === undefined || freq[meta.field] === '') {
      return isFalsyText(value) ? '不合格' : '合格'
    }
    if (meta.type === 'text') {
      const input = normalizeText(value)
      const expected = normalizeText(freq[meta.field])
      return input.includes(expected) || expected.includes(input) ? '合格' : '不合格'
    }
    if (meta.type === 'freq-range') {
      return compareFrequencyContainment(value, freq[meta.field]) || ''
    }
    if (meta.type === 'region') {
      return compareRegionContainment(value, freq[meta.field]) || ''
    }
    if (meta.type === 'number') {
      const expected = freq[meta.field]
      if (expected === null || expected === undefined) return ''
      const inputNum = parseFloat(String(value).replace(/[^\d.\-]/g, ''))
      const expectedNum = Number(expected)
      if (Number.isNaN(inputNum) || Number.isNaN(expectedNum)) return ''
      return Math.abs(inputNum - expectedNum) < Math.max(0.01 * expectedNum, 0.1) ? '合格' : '不合格'
    }
    if (meta.type === 'date') {
      const inputDate = parseDateOnly(value)
      const expectedDate = parseDateOnly(freq[meta.field])
      if (!inputDate || !expectedDate) return ''
      return inputDate <= expectedDate ? '合格' : '不合格'
    }
    if (meta.type === 'min-number') {
      const inputNum = Number(value)
      const expectedNum = Number(freq[meta.field])
      if (Number.isNaN(inputNum) || Number.isNaN(expectedNum)) return ''
      return inputNum >= expectedNum ? '合格' : '不合格'
    }
    if (meta.type === 'amount') {
      const expectedPaid = Boolean(freq[meta.field])
      const amount = Number(value)
      if (Number.isNaN(amount)) return ''
      if (expectedPaid) {
        return amount > 0 ? '合格' : '不合格'
      }
      return amount > 0 ? '不合格' : '合格'
    }
    if (meta.type === 'boolean') {
      const expected = Boolean(freq[meta.field])
      const inputMatch = expected ? isTruthyText(value) : isFalsyText(value)
      return inputMatch ? '合格' : '不合格'
    }
  }

  if (stationFieldMap.has(fieldKey)) {
    const meta = stationFieldMap.get(fieldKey)
    const station = stationMap.value[form.stationId] || {}
    if (meta.type === 'coordinates') {
      const lon = station.longitude
      const lat = station.latitude
      // 从用户输入中提取经度、纬度数字
      const nums = normalizeText(value).match(/-?\d+\.?\d*/g)
      if (!nums || nums.length < 2) {
        // 台站也未登记坐标：无法判定，填写即合格
        if (lon == null || lat == null) return normalizeText(value) ? '合格' : ''
        return '不合格'
      }
      const inputLon = parseFloat(nums[0])
      const inputLat = parseFloat(nums[1])
      // 台站登记了坐标时：数值容差比较（容差 0.01 度，约 1km）
      if (lon != null && lat != null) {
        const lonOk = Math.abs(inputLon - Number(lon)) < 0.01
        const latOk = Math.abs(inputLat - Number(lat)) < 0.01
        return lonOk && latOk ? '合格' : '不合格'
      }
      // 台站未登记坐标时：用户填写了有效坐标即合格
      return '合格'
    }
    if (meta.type === 'number') {
      const expected = station[meta.field]
      // 台站登记了数值时：容差比对；未登记时：用户填写有效数值即合格
      if (expected !== null && expected !== undefined) {
        const inputNum = parseFloat(String(value).replace(/[^\d.\-]/g, ''))
        const expectedNum = Number(expected)
        if (Number.isNaN(inputNum) || Number.isNaN(expectedNum)) return ''
        return Math.abs(inputNum - expectedNum) < Math.max(0.01 * expectedNum, 0.1) ? '合格' : '不合格'
      }
      return value !== '' && !Number.isNaN(Number(value)) ? '合格' : ''
    }
    const expected = station[meta.field]
    // 台站登记了文本时：包含比对；未登记时：用户填写有效文本即合格
    if (expected) {
      const input = normalizeText(value)
      const expectedText = normalizeText(expected)
      return input.includes(expectedText) || expectedText.includes(input) ? '合格' : '不合格'
    }
    return normalizeText(value) ? '合格' : ''
  }

  if (['check_2_1_1_1', 'check_2_2_1_1', 'check_2_3_1_1'].includes(fieldKey)) {
    // 优先基于用户输入的实际值判定：有效期 >= 今天 → 合格
    const inputDate = parseDateOnly(value)
    if (inputDate) {
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      return inputDate >= today ? '合格' : '不合格'
    }
    // 回退：用台站登记的有效期判定
    const station = stationMap.value[form.stationId] || {}
    const validUntil = parseDateOnly(station.validUntil)
    if (!validUntil) return ''
    const today2 = new Date()
    today2.setHours(0, 0, 0, 0)
    return validUntil >= today2 ? '合格' : '不合格'
  }

  // 规则判定：基于字段语义进行自动判别
  const label = getFieldMetaByKey(fieldKey)?.label || ''
  const fieldDef = getFieldMetaByKey(fieldKey) || { key: fieldKey, label: '' }
  const fieldType = getFieldMeta(fieldDef).type

  // 选择型字段：根据选项语义自动判定
  if (fieldType === 'select') {
    const val = normalizeText(value)
    // 有自定义选项时，判断选项的语义
    if (customSelectOptions[fieldKey]) {
      const opts = customSelectOptions[fieldKey]
      const normalizedOpts = opts.map(o => normalizeText(o.value))
      // 多选项分类字段（用途、业务类别等，选项均≥3个且为分类值）：任意合法选项 = 合格
      if (opts.length > 2) {
        return normalizedOpts.includes(val) ? '合格' : '不合格'
      }
      // 二选项字段：第一项为正向(合格)，第二项为负向(不合格)
      if (val === normalizedOpts[0]) return '合格'
      if (opts.length > 1 && val === normalizedOpts[1]) return '不合格'
    }
    // 通用规则回退
    if (label.includes('有害干扰')) {
      if (val === '无') return '合格'
      if (val === '有') return '不合格'
      if (val === '否') return '合格'
      if (val === '是') return '不合格'
    }
    if (val === '符合') return '合格'
    if (val === '不符合') return '不合格'
    if (val === '已报送') return '合格'
    if (val === '未报送') return '不合格'
    // 正向语义：已/是/有/合格/存在/报送 → 合格
    if (isTruthyText(value)) return '合格'
    if (isFalsyText(value)) return '不合格'
  }

  // 日期型字段：填写了即合格
  if (fieldType === 'date') {
    return parseDateOnly(value) ? '合格' : ''
  }

  // 数值型字段（金额、百分比）：有数值即合格
  if (['number', 'money', 'percent'].includes(fieldType)) {
    return value !== '' && !Number.isNaN(Number(value)) ? '合格' : ''
  }

  // 文本型字段：有内容即合格
  if (fieldType === 'text') {
    // 如果该字段有自定义选项，即使 getFieldMeta 返回了 text，也应做语义判定
    if (customSelectOptions[fieldKey]) {
      const opts = customSelectOptions[fieldKey]
      const normalizedOpts = opts.map(o => normalizeText(o.value))
      const val = normalizeText(value)
      if (opts.length > 2) {
        return normalizedOpts.includes(val) ? '合格' : '不合格'
      }
      if (val === normalizedOpts[0]) return '合格'
      if (opts.length > 1 && val === normalizedOpts[1]) return '不合格'
    }
    return normalizeText(value) ? '合格' : ''
  }

  if (isFalsyText(value)) return '不合格'
  return '合格'
}
</script>

<style scoped>
.mb-12 {
  margin-bottom: 12px;
}

.mt-12 {
  margin-top: 12px;
}

.detail-table {
  width: 100%;
}

.detail-table .el-table__cell {
  padding: 6px 8px;
}

.detail-table-input,
.detail-table-select {
  width: 100%;
}

.detail-inline-unit {
  margin-left: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.auto-result-tag {
  min-width: 56px;
  text-align: center;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.basic-filter-input {
  width: 280px;
}

.basic-filter-select {
  width: 130px;
}

.text-muted {
  color: var(--text-secondary);
}

:deep(.new-task-row) {
  animation: highlight-blink 1.2s ease-in-out 2;
}

@keyframes highlight-blink {
  0% { background-color: #fef0f0; }
  50% { background-color: transparent; }
  100% { background-color: #fef0f0; }
}

:deep(.el-table__body tr.new-task-row td) {
  animation: highlight-blink 1.2s ease-in-out 2;
}

.history-action {
  font-weight: 600;
}

.history-detail {
  color: var(--text-secondary);
  margin-top: 4px;
}

.result-section {
  margin-bottom: 12px;
}

.result-pdf-content {
  background: #ffffff;
  padding: 8px 8px 0;
}

.result-pdf-content.pdf-exporting :deep(.pdf-tag),
.result-pdf-content.pdf-exporting :deep(.pdf-tag .el-tag__content) {
  color: #303133;
  background-color: #f5f7fa;
  border-color: #dcdfe6;
}

.pdf-tag-text {
  display: inline-block;
  padding: 0 8px;
  font-size: 12px;
  line-height: 20px;
  border-radius: 10px;
  border: 1px solid transparent;
}

.pdf-tag-wrap {
  display: flex;
  justify-content: center;
}

.pdf-tag-success {
  color: #67c23a;
  background-color: #f0f9eb;
  border-color: #c2e7b0;
}

.pdf-tag-danger {
  color: #f56c6c;
  background-color: #fef0f0;
  border-color: #fbc4c4;
}

.pdf-tag-info {
  color: #909399;
  background-color: #f4f4f5;
  border-color: #d3d4d6;
}

.result-section :deep(.el-descriptions__label) {
  font-weight: 600;
  width: 120px;
}
</style>
