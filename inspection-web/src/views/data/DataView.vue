<template>
  <div class="page-card">
    <div class="page-title">数据管理</div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="台站信息管理" name="station">
        <el-space class="mb-12" wrap>
          <el-input v-model="stationQuery.keyword" placeholder="台站名称/识别码/执照号/地址" clearable style="width: 260px" />
          <el-select v-model="stationQuery.stationClass" placeholder="附件分类" clearable style="width: 120px">
            <el-option label="一类" value="一类" />
            <el-option label="二类" value="二类" />
            <el-option label="三类" value="三类" />
          </el-select>
          <el-select v-model="stationQuery.stationType" placeholder="台站类型" clearable style="width: 200px">
            <el-option v-for="item in allStationTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="stationQuery.reviewStatus" placeholder="审核状态" clearable style="width: 120px">
            <el-option label="待审核" value="待审核" />
            <el-option label="已审核" value="已审核" />
          </el-select>
          <el-button type="primary" @click="searchStations">查询</el-button>
          <el-button @click="resetStations">重置</el-button>
          <el-button @click="downloadStations">导出CSV</el-button>
          <el-upload :show-file-list="false" :auto-upload="false" :on-change="importStations">
            <el-button>导入CSV</el-button>
          </el-upload>
          <el-button v-if="canEdit" @click="openStationDialog()">新增台站</el-button>
          <el-button v-if="canReview" @click="reviewSelectedStations('已审核')">批量审核通过</el-button>
        </el-space>

        <el-table :data="stationTable" stripe border @selection-change="onStationSelectionChange" :header-cell-class-name="getStationHeaderClass">
          <el-table-column type="selection" width="48" />
          <el-table-column label="台站名称" min-width="150">
            <template #default="scope">
              <el-link type="primary" @click="openStationDetail(scope.row.id)">{{ scope.row.name }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="stationClass" label="附件分类" width="100" />
          <el-table-column prop="stationType" label="台站类型" min-width="180" show-overflow-tooltip />
          <el-table-column prop="serviceType" label="业务类型" width="130" />
          <el-table-column prop="stationCode" label="识别码" width="120" />
          <el-table-column prop="licenseNo" label="执照号" width="130" />
          <el-table-column label="有效期" width="110">
            <template #default="scope">{{ formatDateOnly(scope.row.validUntil) }}</template>
          </el-table-column>
          <el-table-column prop="address" label="台址/区域" min-width="150" show-overflow-tooltip />
          <el-table-column label="经度" width="90">
            <template #default="scope">{{ scope.row.longitude != null ? scope.row.longitude : '-' }}</template>
          </el-table-column>
          <el-table-column label="纬度" width="90">
            <template #default="scope">{{ scope.row.latitude != null ? scope.row.latitude : '-' }}</template>
          </el-table-column>
          <el-table-column label="发射功率(dBm)" width="100">
            <template #default="scope">{{ scope.row.transmitPower != null ? scope.row.transmitPower : '-' }}</template>
          </el-table-column>
          <el-table-column label="占用带宽(kHz)" width="110">
            <template #default="scope">{{ scope.row.occupiedBandwidth != null ? scope.row.occupiedBandwidth : '-' }}</template>
          </el-table-column>
          <el-table-column label="极化方式" width="100">
            <template #default="scope">{{ scope.row.polarization || '-' }}</template>
          </el-table-column>
          <el-table-column label="天线增益(dBi)" width="100">
            <template #default="scope">{{ scope.row.antennaGain != null ? scope.row.antennaGain : '-' }}</template>
          </el-table-column>
          <el-table-column label="天线高度(m)" width="100">
            <template #default="scope">{{ scope.row.antennaHeight != null ? scope.row.antennaHeight : '-' }}</template>
          </el-table-column>
          <el-table-column label="天线尺寸(m)" width="100">
            <template #default="scope">{{ scope.row.antennaSize != null ? scope.row.antennaSize : '-' }}</template>
          </el-table-column>
          <el-table-column label="最大EIRP(dBm)" width="110">
            <template #default="scope">{{ scope.row.maxEirp != null ? scope.row.maxEirp : '-' }}</template>
          </el-table-column>
          <el-table-column label="轨道信息" width="120" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.orbitalPosition || '-' }}</template>
          </el-table-column>
          <el-table-column label="总带宽(kHz)" width="100">
            <template #default="scope">{{ scope.row.totalBandwidth != null ? scope.row.totalBandwidth : '-' }}</template>
          </el-table-column>
          <el-table-column prop="reviewStatus" label="审核状态" width="100" />
          <el-table-column v-if="showAction" label="操作" width="160" fixed="right">
            <template #default="scope">
              <el-button v-if="canEdit" size="small" text type="primary" @click="openStationDialog(scope.row)">编辑</el-button>
              <el-button v-if="canDelete" size="small" text type="danger" @click="removeStation(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          class="mt-12"
          background
          layout="total, prev, pager, next"
          :total="stationTotal"
          v-model:current-page="stationQuery.page"
          :page-size="stationQuery.size"
          @current-change="loadStations"
        />
      </el-tab-pane>

      <el-tab-pane label="频率使用信息管理" name="frequency">
        <el-space class="mb-12" wrap>
          <el-input v-model="frequencyQuery.keyword" placeholder="许可编号/频率范围/地域/用途" clearable style="width: 280px" />
          <el-input v-model="frequencyQuery.stationId" placeholder="台站ID" clearable style="width: 110px" />
          <el-select v-model="frequencyQuery.reviewStatus" placeholder="审核状态" clearable style="width: 120px">
            <el-option label="待审核" value="待审核" />
            <el-option label="已审核" value="已审核" />
          </el-select>
          <el-select v-model="frequencyQuery.annualReportSubmitted" placeholder="年报" clearable style="width: 110px">
            <el-option label="已报送" value="true" />
            <el-option label="未报送" value="false" />
          </el-select>
          <el-select v-model="frequencyQuery.spectrumFeePaid" placeholder="频占费" clearable style="width: 110px">
            <el-option label="已缴纳" value="true" />
            <el-option label="未缴纳" value="false" />
          </el-select>
          <el-button type="primary" @click="searchFrequencies">查询</el-button>
          <el-button @click="resetFrequencies">重置</el-button>
          <el-button v-if="canEdit" @click="openFrequencyDialog()">新增频率信息</el-button>
          <el-upload :show-file-list="false" :auto-upload="false" :on-change="importFrequencies">
            <el-button>导入CSV</el-button>
          </el-upload>
          <el-button v-if="canEdit" @click="downloadFrequencies">导出CSV</el-button>
          <el-button v-if="canReview" @click="reviewSelectedFrequencies('已审核')">批量审核通过</el-button>
        </el-space>

        <el-table :data="frequencyTable" stripe border @selection-change="onFrequencySelectionChange" :header-cell-class-name="getFrequencyHeaderClass">
          <el-table-column type="selection" width="48" />
          <el-table-column label="关联台站" min-width="140">
            <template #default="scope">
              <el-link type="primary" @click="openStationDetail(scope.row.stationId)">{{ resolveStationName(scope.row.stationId) }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="licenseNo" label="频率许可编号" width="140" />
          <el-table-column prop="frequencyRange" label="使用频率" width="140" />
          <el-table-column prop="usageRegion" label="使用地域" min-width="140" />
          <el-table-column prop="businessUsage" label="业务用途" width="130" />
          <el-table-column label="使用期限" width="110">
            <template #default="scope">{{ formatDateOnly(scope.row.usageDeadline) }}</template>
          </el-table-column>
          <el-table-column prop="reviewStatus" label="审核状态" width="100" />
          <el-table-column prop="usageRate" label="使用率" width="90">
            <template #default="scope">{{ scope.row.usageRate != null ? scope.row.usageRate + '%' : '-' }}</template>
          </el-table-column>
          <el-table-column label="EIRP谱密度(dBm)" width="120">
            <template #default="scope">{{ scope.row.eirpSpectralDensity != null ? scope.row.eirpSpectralDensity : '-' }}</template>
          </el-table-column>
          <el-table-column label="主站天线尺寸(m)" width="120">
            <template #default="scope">{{ scope.row.antennaSize != null ? scope.row.antennaSize : '-' }}</template>
          </el-table-column>
          <el-table-column prop="annualReportSubmitted" label="年报" width="80">
            <template #default="scope">{{ scope.row.annualReportSubmitted ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column prop="spectrumFeePaid" label="频占费" width="80">
            <template #default="scope">{{ scope.row.spectrumFeePaid ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column v-if="showAction" label="操作" width="160" fixed="right">
            <template #default="scope">
              <el-button v-if="canEdit" size="small" text type="primary" @click="openFrequencyDialog(scope.row)">编辑</el-button>
              <el-button v-if="canDelete" size="small" text type="danger" @click="removeFrequency(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          class="mt-12"
          background
          layout="total, prev, pager, next"
          :total="frequencyTotal"
          v-model:current-page="frequencyQuery.page"
          :page-size="frequencyQuery.size"
          @current-change="loadFrequencies"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="stationVisible" title="台站信息" width="820px">
      <el-form :model="stationForm" label-width="120px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="台站名称"><el-input v-model="stationForm.name" placeholder="请输入台站名称" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="台站编码"><el-input v-model="stationForm.stationCode" placeholder="识别码/呼号" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="附件分类"><el-select v-model="stationForm.stationClass" placeholder="请选择" @change="onClassChange"><el-option label="一类" value="一类" /><el-option label="二类" value="二类" /><el-option label="三类" value="三类" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="台站类型"><el-select v-model="stationForm.stationType" placeholder="请选择"><el-option v-for="item in stationTypeOptions" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="业务类型"><el-select v-model="stationForm.serviceType" placeholder="请选择"><el-option label="地面无线电业务" value="地面无线电业务" /><el-option label="空间无线电业务" value="空间无线电业务" /><el-option label="卫星地球站业务" value="卫星地球站业务" /><el-option label="公众移动通信" value="公众移动通信" /></el-select></el-form-item></el-col>
        </el-row>

        <el-divider content-position="left">许可信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="执照号"><el-input v-model="stationForm.licenseNo" placeholder="无线电台执照号" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="有效期"><el-date-picker v-model="stationForm.validUntil" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" :disabled-date="disabledPastDate" /></el-form-item></el-col>
        </el-row>

        <el-divider content-position="left">地理信息</el-divider>
        <el-form-item label="台址/区域"><el-input v-model="stationForm.address" placeholder="台站详细地址或使用区域" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="经度"><el-input-number v-model="stationForm.longitude" :min="-180" :max="180" :precision="6" placeholder="如 116.397428" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="纬度"><el-input-number v-model="stationForm.latitude" :min="-90" :max="90" :precision="6" placeholder="如 39.90923" style="width: 100%" /></el-form-item></el-col>
        </el-row>

        <el-divider content-position="left">技术参数</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="发射功率(dBm)"><el-input-number v-model="stationForm.transmitPower" :precision="2" :step="1" placeholder="请输入" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="占用带宽(kHz)"><el-input-number v-model="stationForm.occupiedBandwidth" :precision="2" :step="1" placeholder="请输入" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="天线增益(dBi)"><el-input-number v-model="stationForm.antennaGain" :precision="2" :step="0.5" placeholder="请输入" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="极化方式"><el-select v-model="stationForm.polarization" placeholder="请选择极化方式" clearable><el-option label="水平" value="水平" /><el-option label="垂直" value="垂直" /><el-option label="左旋圆极化" value="左旋圆极化" /><el-option label="右旋圆极化" value="右旋圆极化" /><el-option label="线极化" value="线极化" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="天线距地高度(m)"><el-input-number v-model="stationForm.antennaHeight" :precision="2" :step="1" :min="0" placeholder="请输入" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="天线尺寸(m)"><el-input-number v-model="stationForm.antennaSize" :precision="2" :step="0.5" :min="0" placeholder="请输入" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="最大EIRP(dBm)"><el-input-number v-model="stationForm.maxEirp" :precision="2" :step="1" placeholder="请输入" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="总带宽(kHz)"><el-input-number v-model="stationForm.totalBandwidth" :precision="2" :step="1" placeholder="请输入" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="轨道信息"><el-input v-model="stationForm.orbitalPosition" placeholder="如 东经110.5° 或 LEO" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="stationVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStation">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="frequencyVisible" title="频率使用信息" width="720px">
      <el-form :model="frequencyForm" label-width="130px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="关联台站"><el-select v-model="frequencyForm.stationId" placeholder="请选择台站"><el-option v-for="item in stationOptionsAll" :key="item.stationId" :label="`${item.stationId} - ${item.name}`" :value="item.stationId" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="频率许可编号"><el-input v-model="frequencyForm.licenseNo" placeholder="许可编号" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="使用频率"><el-select v-model="frequencyForm.frequencyRange" placeholder="请选择或输入" allow-create filterable default-first-option><el-option v-for="item in frequencyRangeOptions" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="使用地域"><el-select v-model="frequencyForm.usageRegion" placeholder="请选择或输入" allow-create filterable><el-option v-for="item in usageRegionOptions" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="业务用途"><el-select v-model="frequencyForm.businessUsage" placeholder="请选择或输入" allow-create filterable><el-option v-for="item in businessUsageOptions" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="使用期限"><el-date-picker v-model="frequencyForm.usageDeadline" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" /></el-form-item></el-col>
        </el-row>

        <el-divider content-position="left">技术参数</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="EIRP谱密度(dBm)"><el-input-number v-model="frequencyForm.eirpSpectralDensity" :precision="2" :step="0.5" placeholder="请输入" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="主站天线尺寸(m)"><el-input-number v-model="frequencyForm.antennaSize" :precision="2" :step="0.5" :min="0" placeholder="请输入" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="技术方案"><el-select v-model="frequencyForm.technicalScheme" placeholder="请选择或输入" allow-create filterable><el-option v-for="item in technicalSchemeOptions" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="技术人员"><el-select v-model="frequencyForm.servicePersonnel" placeholder="请选择或输入" allow-create filterable><el-option v-for="item in servicePersonnelOptions" :key="item" :label="item" :value="item" /></el-select></el-form-item>

        <el-divider content-position="left">管理信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="年报报送"><el-switch v-model="frequencyForm.annualReportSubmitted" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="频占费缴纳"><el-switch v-model="frequencyForm.spectrumFeePaid" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="干扰标记"><el-switch v-model="frequencyForm.interferenceFlag" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="审核状态"><el-input v-model="frequencyForm.reviewStatus" disabled /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="使用率(%)"><el-input-number v-model="frequencyForm.usageRate" :min="0" :max="100" :step="0.1" placeholder="检查后回填" style="width: 100%" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="frequencyVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFrequency">保存</el-button>
      </template>
    </el-dialog>

    <StationDetailDialog v-model="stationDetailVisible" :station-id="selectedStationId" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createFrequency, deleteFrequency, exportFrequencies, fetchFrequencies, importFrequenciesCsv, reviewFrequencies, updateFrequency } from '../../api/frequencies'
import { createStation, deleteStation, exportStations, fetchStations, importStationsCsv, reviewStations, updateStation } from '../../api/stations'
import { useAuthStore } from '../../stores/auth'
import StationDetailDialog from '../../components/StationDetailDialog.vue'

const activeTab = ref('station')
const auth = useAuthStore()
const role = computed(() => auth.user?.role || 'OPERATOR')
const canEdit = computed(() => ['ADMIN', 'OPERATOR'].includes(role.value))
const canDelete = computed(() => role.value === 'ADMIN')
const canReview = computed(() => ['ADMIN', 'OPERATOR'].includes(role.value))
const showAction = computed(() => canEdit.value || canDelete.value)

const stationTypeRule = {
  一类: ['广播电台', '微波站', '雷达站', '静止轨道空间无线电台', '卫星测控（导航）站', '卫星关口站', '卫星国际专线地球站'],
  二类: ['公众移动通信基站（含室外直放站，不含室内站）'],
  三类: ['其他地面无线电业务台（站）', '非静止轨道空间无线电台', '其他卫星地球站']
}
const allStationTypeOptions = Array.from(new Set(Object.values(stationTypeRule).flat()))

const frequencyRangeOptions = ['98.1MHz-99.5MHz', '7.2GHz-7.4GHz', '2.9GHz-3.1GHz', '3.5GHz', '162MHz-174MHz', '14GHz/12GHz', '6.4GHz-6.7GHz']
const usageRegionOptions = ['北京市海淀区', '北京市朝阳区', '天津滨海新区', '河北保定市', '河北石家庄市', '河北唐山市', '河北廊坊市', '河北秦皇岛市']
const businessUsageOptions = ['广播传输', '微波回传', '港航雷达监测', '5G公众移动通信', '港口调度通信', '空间测控', '卫星地球站上下行', '卫星测控（导航）']
const technicalSchemeOptions = ['双机热备广播发射方案', '点对点链路冗余', '雷达扫描参数优化方案', '5G宏站覆盖方案', '同频邻区功率平衡方案', '调度台站抗干扰方案', '地球站上下行链路方案', '测控链路容灾方案']
const servicePersonnelOptions = ['陈敏', '张勇', '李涛', '孙平', '赵林', '刘凯', '郭峰', '周晨', '韩博', '郑楠']

const stationQuery = reactive({ page: 1, size: 10, keyword: '', stationClass: '', stationType: '', reviewStatus: '' })
const frequencyQuery = reactive({ page: 1, size: 10, keyword: '', stationId: '', reviewStatus: '', annualReportSubmitted: '', spectrumFeePaid: '' })

const stationTable = ref([])
const stationOptionsAll = ref([])
const stationNameMap = ref({})
const frequencyTable = ref([])
const stationTotal = ref(0)
const frequencyTotal = ref(0)
const selectedStationIds = ref([])
const selectedFrequencyIds = ref([])

const stationVisible = ref(false)
const frequencyVisible = ref(false)
const stationDetailVisible = ref(false)
const selectedStationId = ref(null)
const stationTypeOptions = ref(allStationTypeOptions)

const resolveStationName = (stationId) => {
  if (!stationId) return ''
  return stationNameMap.value[stationId] || `台站-${stationId}`
}

const stationForm = reactive({
  id: null,
  name: '',
  category: '',
  stationClass: '',
  stationType: '',
  serviceType: '',
  stationCode: '',
  address: '',
  licenseNo: '',
  validUntil: '',
  longitude: null,
  latitude: null,
  transmitPower: null,
  occupiedBandwidth: null,
  antennaGain: null,
  antennaHeight: null,
  antennaSize: null,
  polarization: '',
  maxEirp: null,
  orbitalPosition: '',
  totalBandwidth: null
})

const frequencyForm = reactive({
  id: null,
  stationId: null,
  licenseNo: '',
  frequencyRange: '',
  usageRegion: '',
  businessUsage: '',
  usageDeadline: '',
  usageRate: null,
  eirpSpectralDensity: null,
  antennaSize: null,
  annualReportSubmitted: true,
  spectrumFeePaid: true,
  interferenceFlag: false,
  technicalScheme: '',
  servicePersonnel: '',
  reviewStatus: '待审核'
})

const onClassChange = (stationClass) => {
  stationTypeOptions.value = stationClass ? (stationTypeRule[stationClass] || []) : allStationTypeOptions
  stationForm.stationType = ''
}

const disabledPastDate = (time) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}

const formatDateOnly = (value) => {
  if (!value) return ''
  if (Array.isArray(value)) {
    const [year, month, day] = value
    if (!year || !month || !day) return ''
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  const raw = String(value).replace('T', ' ')
  if (/^\d{4}-\d{2}-\d{2}/.test(raw)) {
    return raw.slice(0, 10)
  }
  const parts = raw.split(/[-/:,\s]/).filter(Boolean)
  if (parts.length >= 3 && parts[0].length === 4) {
    return `${parts[0]}-${String(parts[1]).padStart(2, '0')}-${String(parts[2]).padStart(2, '0')}`
  }
  return raw.slice(0, 10)
}

const getStationHeaderClass = ({ columnIndex }) => {
  if (columnIndex === 0) return ''
  if (columnIndex <= 5) return 'col-basic'
  if (columnIndex <= 7) return 'col-license'
  if (columnIndex <= 10) return 'col-location'
  if (columnIndex <= 19) return 'col-antenna'
  if (columnIndex <= 20) return 'col-manage'
  return ''
}

const getFrequencyHeaderClass = ({ columnIndex }) => {
  if (columnIndex === 0) return ''
  if (columnIndex <= 5) return 'col-basic'
  if (columnIndex <= 7) return 'col-license'
  if (columnIndex <= 10) return 'col-tech'
  if (columnIndex <= 12) return 'col-manage'
  return ''
}

const loadStations = async () => {
  const params = {
    ...stationQuery,
    category: stationQuery.stationClass,
    stationType: stationQuery.stationType || undefined
  }
  const res = await fetchStations(params)
  if (res.success) {
    const records = Array.isArray(res.data) ? res.data : res.data.records
    stationTable.value = records || []
    stationTotal.value = Array.isArray(res.data) ? stationTable.value.length : res.data.total
  }
}

const loadStationOptions = async () => {
  const res = await fetchStations({ page: 1, size: 1000 })
  if (res.success) {
    const records = Array.isArray(res.data) ? res.data : res.data.records
    stationOptionsAll.value = records || []
    const map = {}
    ;(records || []).forEach((item) => {
      map[item.stationId] = item.name
    })
    stationNameMap.value = map
  }
}

const loadFrequencies = async () => {
  const stationIdNum = frequencyQuery.stationId ? Number(frequencyQuery.stationId) : null
  const params = {
    ...frequencyQuery,
    stationId: Number.isNaN(stationIdNum) ? null : stationIdNum,
    annualReportSubmitted: frequencyQuery.annualReportSubmitted === '' ? null : frequencyQuery.annualReportSubmitted === 'true',
    spectrumFeePaid: frequencyQuery.spectrumFeePaid === '' ? null : frequencyQuery.spectrumFeePaid === 'true'
  }
  const res = await fetchFrequencies(params)
  if (res.success) {
    const records = Array.isArray(res.data) ? res.data : res.data.records
    frequencyTable.value = records || []
    frequencyTotal.value = Array.isArray(res.data) ? frequencyTable.value.length : res.data.total
  }
}

const searchStations = () => {
  stationQuery.page = 1
  loadStations()
}

const searchFrequencies = () => {
  frequencyQuery.page = 1
  loadFrequencies()
}

const resetStations = () => {
  Object.assign(stationQuery, { page: 1, size: 10, keyword: '', stationClass: '', stationType: '', reviewStatus: '' })
  loadStations()
}

const resetFrequencies = () => {
  Object.assign(frequencyQuery, { page: 1, size: 10, keyword: '', stationId: '', reviewStatus: '', annualReportSubmitted: '', spectrumFeePaid: '' })
  loadFrequencies()
}

const openStationDetail = (stationId) => {
  selectedStationId.value = stationId
  stationDetailVisible.value = true
}

const onStationSelectionChange = (rows) => {
  selectedStationIds.value = rows.map((item) => item.id)
}

const openStationDialog = (row) => {
  if (row) {
    Object.assign(stationForm, {
      id: row.id,
      name: row.name || '',
      category: row.category || row.stationClass || '',
      stationClass: row.stationClass || '',
      stationType: row.stationType || '',
      serviceType: row.serviceType || '',
      stationCode: row.stationCode || '',
      address: row.address || '',
      licenseNo: row.licenseNo || '',
      validUntil: row.validUntil || '',
      longitude: row.longitude != null ? row.longitude : null,
      latitude: row.latitude != null ? row.latitude : null,
      transmitPower: row.transmitPower != null ? row.transmitPower : null,
      occupiedBandwidth: row.occupiedBandwidth != null ? row.occupiedBandwidth : null,
      antennaGain: row.antennaGain != null ? row.antennaGain : null,
      antennaHeight: row.antennaHeight != null ? row.antennaHeight : null,
      antennaSize: row.antennaSize != null ? row.antennaSize : null,
      polarization: row.polarization || '',
      maxEirp: row.maxEirp != null ? row.maxEirp : null,
      orbitalPosition: row.orbitalPosition || '',
      totalBandwidth: row.totalBandwidth != null ? row.totalBandwidth : null
    })
    stationTypeOptions.value = row.stationClass ? (stationTypeRule[row.stationClass] || allStationTypeOptions) : allStationTypeOptions
  } else {
    Object.assign(stationForm, {
      id: null,
      name: '',
      category: '',
      stationClass: '',
      stationType: '',
      serviceType: '',
      stationCode: '',
      address: '',
      licenseNo: '',
      validUntil: '',
      longitude: null,
      latitude: null,
      transmitPower: null,
      occupiedBandwidth: null,
      antennaGain: null,
      antennaHeight: null,
      antennaSize: null,
      polarization: '',
      maxEirp: null,
      orbitalPosition: '',
      totalBandwidth: null
    })
    stationTypeOptions.value = allStationTypeOptions
  }
  stationVisible.value = true
}

const saveStation = async () => {
  if (!stationForm.name || !stationForm.stationClass || !stationForm.stationType || !stationForm.serviceType || !stationForm.stationCode || !stationForm.licenseNo) {
    ElMessage.warning('请完整填写台站必填字段')
    return
  }
  stationForm.category = stationForm.stationClass
  if (stationForm.id) {
    await updateStation(stationForm.id, stationForm)
  } else {
    await createStation(stationForm)
  }
  ElMessage.success('保存成功')
  stationVisible.value = false
  loadStations()
}

const removeStation = async (id) => {
  await deleteStation(id)
  ElMessage.success('删除成功')
  loadStations()
}

const importStations = async (fileObj) => {
  const formData = new FormData()
  formData.append('file', fileObj.raw)
  const res = await importStationsCsv(formData)
  if (res.success) {
    ElMessage.success(`导入成功 ${res.data} 条`)
    loadStations()
  }
}

const downloadStations = async () => {
  const blob = await exportStations()
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'radio-stations.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

const reviewSelectedStations = async (reviewStatus) => {
  if (!selectedStationIds.value.length) {
    ElMessage.warning('请先选择台站')
    return
  }
  await reviewStations({ ids: selectedStationIds.value, reviewStatus, reviewedBy: auth.user?.username || 'operator' })
  ElMessage.success('审核成功')
  loadStations()
}

const openFrequencyDialog = (row) => {
  if (row) {
    Object.assign(frequencyForm, {
      id: row.id || null,
      stationId: row.stationId || null,
      licenseNo: row.licenseNo || '',
      frequencyRange: row.frequencyRange || '',
      usageRegion: row.usageRegion || '',
      businessUsage: row.businessUsage || '',
      usageDeadline: row.usageDeadline || '',
      usageRate: row.usageRate != null ? row.usageRate : null,
      eirpSpectralDensity: row.eirpSpectralDensity != null ? row.eirpSpectralDensity : null,
      antennaSize: row.antennaSize != null ? row.antennaSize : null,
      annualReportSubmitted: row.annualReportSubmitted != null ? row.annualReportSubmitted : true,
      spectrumFeePaid: row.spectrumFeePaid != null ? row.spectrumFeePaid : true,
      interferenceFlag: row.interferenceFlag != null ? row.interferenceFlag : false,
      technicalScheme: row.technicalScheme || '',
      servicePersonnel: row.servicePersonnel || '',
      reviewStatus: row.reviewStatus || '待审核'
    })
  } else {
    Object.assign(frequencyForm, {
      id: null,
      stationId: null,
      licenseNo: '',
      frequencyRange: '',
      usageRegion: '',
      businessUsage: '',
      usageDeadline: '',
      usageRate: null,
      eirpSpectralDensity: null,
      antennaSize: null,
      annualReportSubmitted: true,
      spectrumFeePaid: true,
      interferenceFlag: false,
      technicalScheme: '',
      servicePersonnel: '',
      reviewStatus: '待审核'
    })
  }
  frequencyVisible.value = true
}

const saveFrequency = async () => {
  if (!frequencyForm.stationId || !frequencyForm.licenseNo || !frequencyForm.frequencyRange || !frequencyForm.usageRegion || !frequencyForm.businessUsage) {
    ElMessage.warning('请完整填写频率必填字段')
    return
  }
  if (frequencyForm.id) {
    await updateFrequency(frequencyForm.id, frequencyForm)
  } else {
    await createFrequency(frequencyForm)
  }
  ElMessage.success('保存成功')
  frequencyVisible.value = false
  loadFrequencies()
}

const removeFrequency = async (id) => {
  await deleteFrequency(id)
  ElMessage.success('删除成功')
  loadFrequencies()
}

const importFrequencies = async (fileObj) => {
  const formData = new FormData()
  formData.append('file', fileObj.raw)
  const res = await importFrequenciesCsv(formData)
  if (res.success) {
    ElMessage.success(`导入成功 ${res.data} 条`)
    loadFrequencies()
  }
}

const onFrequencySelectionChange = (rows) => {
  selectedFrequencyIds.value = rows.map((item) => item.id)
}

const downloadFrequencies = async () => {
  const blob = await exportFrequencies()
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'frequency-usage.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

const reviewSelectedFrequencies = async (reviewStatus) => {
  if (!selectedFrequencyIds.value.length) {
    ElMessage.warning('请先选择频率使用信息')
    return
  }
  await reviewFrequencies({ ids: selectedFrequencyIds.value, reviewStatus, reviewedBy: auth.user?.username || 'operator' })
  ElMessage.success('审核成功')
  loadFrequencies()
}

onMounted(async () => {
  await loadStations()
  await loadStationOptions()
  await loadFrequencies()
})
</script>

<style scoped>
.mb-12 {
  margin-bottom: 12px;
}

.mt-12 {
  margin-top: 12px;
}

:deep(.el-divider__text) {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.col-basic) {
  background-color: #ecf5ff !important;
}

:deep(.col-license) {
  background-color: #f0f9eb !important;
}

:deep(.col-location) {
  background-color: #fdf6ec !important;
}

:deep(.col-antenna) {
  background-color: #f3e8ff !important;
}

:deep(.col-tech) {
  background-color: #f3e8ff !important;
}

:deep(.col-manage) {
  background-color: #fef0f0 !important;
}
</style>
