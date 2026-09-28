import { computed, nextTick, reactive, ref, watch } from 'vue'

export function useAttachmentPreview({ api, apiBaseUrl, showToast, formatDateTime, formatFileSize }) {
  const previewLoading = ref(false)
  const preview = reactive({
    open: false,
    attachmentId: null,
    kind: '',
    url: '',
    downloadUrl: '',
    fileName: '',
    fileSize: '',
    contentType: '',
    uploadedAt: '',
    message: '',
    onlyOfficeApiUrl: '',
    onlyOfficeDocumentUrl: '',
    onlyOfficeFileType: '',
    onlyOfficeDocumentKey: '',
  })
  const previewRefreshSeed = ref(0)
  const onlyOfficeMountRef = ref(null)
  let onlyOfficeEditor = null
  let onlyOfficeScriptPromise = null

  function isPreviewFrameKind(kind) {
    return ['office-word', 'office-sheet', 'office-slide', 'markdown', 'text', 'html', 'pdf'].includes(kind)
  }

  function destroyOnlyOfficeEditor() {
    if (onlyOfficeEditor?.destroyEditor) onlyOfficeEditor.destroyEditor()
    onlyOfficeEditor = null
    if (onlyOfficeMountRef.value) onlyOfficeMountRef.value.innerHTML = ''
  }

  function ensureOnlyOfficeScript(url) {
    if (window.DocsAPI?.DocEditor) return Promise.resolve()
    if (onlyOfficeScriptPromise) return onlyOfficeScriptPromise
    onlyOfficeScriptPromise = new Promise((resolve, reject) => {
      const existing = document.querySelector(`script[data-onlyoffice-api="${url}"]`)
      if (existing) {
        existing.addEventListener('load', () => resolve(), { once: true })
        existing.addEventListener('error', () => reject(new Error('OnlyOffice 脚本加载失败')), { once: true })
        return
      }
      const script = document.createElement('script')
      script.src = url
      script.async = true
      script.dataset.onlyofficeApi = url
      script.onload = () => resolve()
      script.onerror = () => reject(new Error('OnlyOffice 脚本加载失败'))
      document.head.appendChild(script)
    })
    return onlyOfficeScriptPromise
  }

  const previewEngineLabel = computed(() => {
    if (preview.kind === 'onlyoffice') return 'OnlyOffice'
    if (preview.kind === 'pdf') return 'PDF 预览'
    if (['office-word', 'office-sheet', 'office-slide'].includes(preview.kind)) return 'Office 转换预览'
    if (['markdown', 'text', 'html'].includes(preview.kind)) return 'HTML 预览'
    if (['image', 'video', 'audio'].includes(preview.kind)) return '原文件预览'
    return '附件预览'
  })

  const previewHint = computed(() => {
    if (preview.kind === 'pdf') return '当前文件已转换为 PDF 预览，适合快速核对版式与分页。'
    if (preview.kind === 'onlyoffice') return '当前文件通过 OnlyOffice 在线预览。'
    if (['office-word', 'office-sheet', 'office-slide'].includes(preview.kind)) return '当前文件已转换为网页可预览内容。'
    if (['markdown', 'text', 'html'].includes(preview.kind)) return '当前文件以文本方式在线展示。'
    if (['image', 'video', 'audio'].includes(preview.kind)) return '当前文件使用原始媒体方式预览。'
    return '当前文件暂不支持复杂预览，可直接下载查看。'
  })

  const previewFrameUrl = computed(() => {
    if (!preview.url || !isPreviewFrameKind(preview.kind) || preview.kind === 'onlyoffice') return preview.url
    const connector = preview.url.includes('?') ? '&' : '?'
    return `${preview.url}${connector}_r=${previewRefreshSeed.value}`
  })

  const canUseOnlyOfficeComponent = computed(() => preview.kind === 'onlyoffice'
    && !!preview.onlyOfficeApiUrl
    && !!preview.onlyOfficeDocumentUrl
    && !!preview.onlyOfficeFileType
    && !!preview.onlyOfficeDocumentKey)

  async function mountOnlyOfficeEditor() {
    if (!canUseOnlyOfficeComponent.value) return
    await nextTick()
    if (!onlyOfficeMountRef.value) return
    try {
      await ensureOnlyOfficeScript(preview.onlyOfficeApiUrl)
      destroyOnlyOfficeEditor()
      onlyOfficeEditor = new window.DocsAPI.DocEditor(onlyOfficeMountRef.value, {
        documentType: preview.kind === 'office-sheet' ? 'cell' : preview.kind === 'office-slide' ? 'slide' : 'word',
        type: 'embedded',
        width: '100%',
        height: '100%',
        document: {
          title: preview.fileName || 'document',
          url: preview.onlyOfficeDocumentUrl,
          fileType: preview.onlyOfficeFileType,
          key: `${preview.onlyOfficeDocumentKey}-${previewRefreshSeed.value}`,
        },
        editorConfig: {
          mode: 'view',
          lang: 'zh-CN',
          customization: { autosave: false, comments: false, compactHeader: true, compactToolbar: true, toolbarHideFileName: false },
        },
      })
    } catch (error) {
      showToast(error?.message || 'OnlyOffice 组件加载失败，已回退为网页预览', 'error')
    }
  }

  async function loadPreviewMeta(attachment) {
    const isSession = !!attachment?.isSessionAttachment
    const base = isSession
      ? `/skills/session-attachments/${attachment.id}`
      : `/attachments/${attachment.id}`
    const { data } = await api.get(`${base}/preview`)
    const meta = data.data || {}
    const siteBaseUrl = apiBaseUrl.replace(/\/api$/, '')
    preview.attachmentId = attachment.id
    preview.kind = meta.kind || ''
    preview.url = /^https?:\/\//.test(meta.previewUrl || '') ? meta.previewUrl : `${siteBaseUrl}${meta.previewUrl || ''}`
    preview.downloadUrl = isSession
      ? `${siteBaseUrl}/api/skills/session-attachments/${attachment.id}/download`
      : `${siteBaseUrl}/api/attachments/${attachment.id}/download`
    preview.fileName = meta.fileName || attachment.originalFileName || '附件'
    preview.fileSize = formatFileSize(meta.fileSize ?? attachment.fileSize)
    preview.contentType = meta.contentType || '未知类型'
    preview.uploadedAt = attachment.uploadedAt ? formatDateTime(attachment.uploadedAt) : '--'
    preview.message = meta.message || ''
    preview.onlyOfficeApiUrl = meta.onlyOfficeApiUrl || ''
    preview.onlyOfficeDocumentUrl = meta.onlyOfficeDocumentUrl || ''
    preview.onlyOfficeFileType = meta.onlyOfficeFileType || ''
    preview.onlyOfficeDocumentKey = meta.onlyOfficeDocumentKey || ''
    previewRefreshSeed.value = Date.now()
  }

  async function openPreview(attachment) {
    preview.open = true
    previewLoading.value = true
    destroyOnlyOfficeEditor()
    try {
      await loadPreviewMeta(attachment)
      if (canUseOnlyOfficeComponent.value) await mountOnlyOfficeEditor()
      // 焦点落在 iframe/embed 内部时，把 Esc 转出来关闭预览
      nextTick(() => bindPreviewEscapeHandlers())
    } catch (error) {
      preview.open = false
      showToast(error?.response?.data?.message || '预览失败', 'error')
    } finally {
      previewLoading.value = false
    }
  }

  /** iframe 内部（同源）+ 父页面：Esc 都能关预览 */
  function onPreviewEsc(e) {
    if (e.key === 'Escape') {
      e.preventDefault()
      e.stopPropagation()
      preview.open = false
    }
  }

  function attachEscToFrame(frame) {
    if (!frame) return
    const attach = () => {
      try {
        frame.contentDocument?.addEventListener('keydown', onPreviewEsc, true)
      } catch { /* 跨域忽略 */ }
      try {
        // PDF 插件可能不透传到 contentDocument，再挂 contentWindow
        frame.contentWindow?.addEventListener('keydown', onPreviewEsc, true)
      } catch { /* 跨域忽略 */ }
    }
    attach()
    frame.addEventListener('load', attach)
  }

  function bindPreviewEscapeHandlers() {
    // 父文档（焦点不在 iframe 时）
    document.addEventListener('keydown', onPreviewEsc, true)
    window.addEventListener('keydown', onPreviewEsc, true)
    document.querySelectorAll('iframe.preview-frame').forEach(attachEscToFrame)
    // embed/object 没有 contentDocument：点击预览区后焦点可能在插件内，
    // 用 window blur→focus 回拉 + 容器 tabindex 保证 Esc 可达
    const stage = document.querySelector('.preview-stage')
    if (stage) {
      stage.tabIndex = -1
      stage.addEventListener('keydown', onPreviewEsc)
    }
  }

  function unbindPreviewEscapeHandlers() {
    document.removeEventListener('keydown', onPreviewEsc, true)
    window.removeEventListener('keydown', onPreviewEsc, true)
  }

  /** iframe load 后把 Esc 转出来（PDF/HTML 预览） */
  function onPreviewFrameLoad(e) {
    attachEscToFrame(e?.target)
  }

  async function reloadPreview() {
    if (!preview.attachmentId) return
    previewLoading.value = true
    destroyOnlyOfficeEditor()
    try {
      await loadPreviewMeta({ id: preview.attachmentId, fileSize: 0 })
      if (canUseOnlyOfficeComponent.value) await mountOnlyOfficeEditor()
      showToast('预览已刷新')
    } catch (error) {
      showToast(error?.response?.data?.message || '刷新预览失败', 'error')
    } finally {
      previewLoading.value = false
    }
  }

  watch(() => preview.open, (open) => {
    if (open) {
      nextTick(() => bindPreviewEscapeHandlers())
    } else {
      unbindPreviewEscapeHandlers()
      destroyOnlyOfficeEditor()
    }
  })

  return {
    previewLoading,
    preview,
    onlyOfficeMountRef,
    previewEngineLabel,
    previewHint,
    previewFrameUrl,
    canUseOnlyOfficeComponent,
    openPreview,
    reloadPreview,
    destroyOnlyOfficeEditor,
    onPreviewFrameLoad,
  }
}
