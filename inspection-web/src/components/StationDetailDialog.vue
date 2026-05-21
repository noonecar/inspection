<template>
  <el-dialog
    :model-value="visible"
    :title="detail ? detail.name : '台站详情'"
    width="900px"
    top="3vh"
    @update:model-value="$emit('update:modelValue', $event)"
    @close="handleClose"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <el-divider content-position="left">基本信息</el-divider>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="台站名称" :span="2">{{ detail.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">{{ detail.reviewStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="附件分类">{{ detail.stationClass || '-' }}</el-descriptions-item>
          <el-descriptions-item label="台站类型">{{ detail.stationType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ detail.serviceType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="识别码">{{ detail.stationCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="执照号">{{ detail.licenseNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="有效期">{{ formatDateOnly(detail.validUntil) || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">地理位置</el-divider>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="台址/区域" :span="3">{{ detail.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="经度">{{ detail.longitude != null ? detail.longitude : '-' }}</el-descriptions-item>
          <el-descriptions-item label="纬度">{{ detail.latitude != null ? detail.latitude : '-' }}</el-descriptions-item>
          <el-descriptions-item label="轨道信息">{{ detail.orbitalPosition || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">技术参数</el-divider>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="发射功率(dBm)">{{ detail.transmitPower != null ? detail.transmitPower : '-' }}</el-descriptions-item>
          <el-descriptions-item label="占用带宽(kHz)">{{ detail.occupiedBandwidth != null ? detail.occupiedBandwidth : '-' }}</el-descriptions-item>
          <el-descriptions-item label="极化方式">{{ detail.polarization || '-' }}</el-descriptions-item>
          <el-descriptions-item label="天线增益(dBi)">{{ detail.antennaGain != null ? detail.antennaGain : '-' }}</el-descriptions-item>
          <el-descriptions-item label="天线高度(m)">{{ detail.antennaHeight != null ? detail.antennaHeight : '-' }}</el-descriptions-item>
          <el-descriptions-item label="天线尺寸(m)">{{ detail.antennaSize != null ? detail.antennaSize : '-' }}</el-descriptions-item>
          <el-descriptions-item label="最大EIRP(dBm)">{{ detail.maxEirp != null ? detail.maxEirp : '-' }}</el-descriptions-item>
          <el-descriptions-item label="总带宽(kHz)">{{ detail.totalBandwidth != null ? detail.totalBandwidth : '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">频率使用信息</el-divider>
        <el-table :data="detail.frequencies" border size="small" max-height="300">
          <el-table-column prop="licenseNo" label="频率许可编号" min-width="140" />
          <el-table-column prop="frequencyRange" label="使用频率" min-width="140" />
          <el-table-column label="使用期限" width="110">
            <template #default="scope">{{ formatDateOnly(scope.row.usageDeadline) }}</template>
          </el-table-column>
          <el-table-column prop="usageRate" label="使用率" width="90">
            <template #default="scope">{{ scope.row.usageRate != null ? scope.row.usageRate + '%' : '-' }}</template>
          </el-table-column>
          <el-table-column label="EIRP谱密度(dBm)" width="120">
            <template #default="scope">{{ scope.row.eirpSpectralDensity != null ? scope.row.eirpSpectralDensity : '-' }}</template>
          </el-table-column>
          <el-table-column label="主站天线尺寸(m)" width="120">
            <template #default="scope">{{ scope.row.antennaSize != null ? scope.row.antennaSize : '-' }}</template>
          </el-table-column>
          <el-table-column label="年报" width="70">
            <template #default="scope">{{ scope.row.annualReportSubmitted ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="频占费" width="80">
            <template #default="scope">{{ scope.row.spectrumFeePaid ? '是' : '否' }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!detail.frequencies || detail.frequencies.length === 0" description="暂无频率使用记录" />
      </template>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { fetchStationDetail } from '../api/stations'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  stationId: { type: Number, default: null }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const detail = ref(null)
const loading = ref(false)

const formatDateOnly = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 10)
}

const loadDetail = async () => {
  if (!props.stationId) return
  detail.value = null
  loading.value = true
  try {
    const res = await fetchStationDetail(props.stationId)
    if (res && res.success) {
      detail.value = res.data
    }
  } finally {
    loading.value = false
  }
}

watch(() => props.stationId, () => {
  if (visible.value) {
    loadDetail()
  }
})

watch(visible, (val) => {
  if (val) {
    loadDetail()
  }
})

const handleClose = () => {
  detail.value = null
}
</script>