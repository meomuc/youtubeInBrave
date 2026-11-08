(function(){
  if (window.__ytTabletHooked) return;
  window.__ytTabletHooked = true;
  // Trong PiP người dùng không thể bấm điều khiển của YouTube, nên mọi lệnh pause của YouTube bị chặn.
  // Nút của app (toggle.js, MediaSessionRules) đặt __ytTabletUserPaused trước khi pause, nên vẫn được phép.
  try {
    var mediaProto = HTMLMediaElement.prototype;
    var origPause = mediaProto.pause;
    mediaProto.pause = function(){
      if (this.tagName === 'VIDEO' && window.__ytTabletPip && !window.__ytTabletUserPaused) return;
      return origPause.apply(this, arguments);
    };
  } catch (e) {}
  // YouTube tự tạm dừng khi trang bị ẩn. Giả lập trang luôn hiển thị để video tiếp tục khi xuống nền.
  try {
    Object.defineProperty(document, 'hidden', {configurable: true, get: function(){ return false; }});
    Object.defineProperty(document, 'visibilityState', {configurable: true, get: function(){ return 'visible'; }});
  } catch (e) {}
  document.addEventListener('visibilitychange', function(e){ e.stopImmediatePropagation(); }, true);
  // Khi app xuống nền, WebView có thể tự tạm dừng video. Nếu không phải do người dùng bấm tạm dừng
  // (cờ __ytTabletUserPaused từ nút PiP), phát lại ngay.
  // Giới hạn: tối đa 3 lần tự phát lại trong 2 giây. Nếu video bị pause liên tục thì dừng ép phát,
  // tránh vòng pause/play làm đứt tiếng (log PiP cho thấy vòng lặp ~0,2 giây mỗi lần).
  var autoResumeTimes = [];
  document.addEventListener('pause', function(e){
    var v = e.target;
    if (window.__ytTabletBg && !window.__ytTabletUserPaused && v && v.tagName === 'VIDEO') {
      var now = Date.now();
      autoResumeTimes = autoResumeTimes.filter(function(t){ return now - t < 2000; });
      if (autoResumeTimes.length >= 3) return;
      autoResumeTimes.push(now);
      v.play().catch(function(){});
    }
  }, true);
  function report(){
    var v = document.querySelector('video');
    var playing = !!(v && !v.paused && !v.ended);
    // Đang phát thì xóa cờ "người dùng tạm dừng" để lần sau tự phát lại ở nền vẫn hoạt động.
    if (playing) window.__ytTabletUserPaused = false;
    // Hệ thống tạm dừng khi xuống nền: không báo, để trang tự phát lại (tránh dừng rồi bật lại service).
    if (!playing && window.__ytTabletBg && !window.__ytTabletUserPaused) return;
    AndroidPlayback.onState(playing);
  }
  ['play', 'playing', 'pause', 'ended'].forEach(function(e){ document.addEventListener(e, report, true); });
  // Ticket #16: ghi vị trí mỗi 5 giây khi đang phát, và ngay khi tạm dừng hoặc kết thúc.
  function currentVideoId(){
    try { return new URL(location.href).searchParams.get('v'); } catch (e) { return null; }
  }
  function pageTitle(){
    return document.title.replace(/^\(\d+\)\s*/, '').replace(/\s*-\s*YouTube$/, '');
  }
  function saveProgress(){
    var v = document.querySelector('video');
    if (!v || location.pathname !== '/watch') return;
    var id = currentVideoId();
    if (v.ended) { AndroidPlayback.onEnded(id); return; }
    AndroidPlayback.onProgress(id, pageTitle(), v.currentTime, v.duration);
  }
  setInterval(function(){
    var v = document.querySelector('video');
    if (v && !v.paused && !v.ended) saveProgress();
  }, 5000);
  ['pause', 'ended'].forEach(function(e){ document.addEventListener(e, saveProgress, true); });
  // Ticket #14: khi hàng đợi bật, chặn sự kiện kết thúc để YouTube không tự phát gợi ý;
  // app tự phát mục tiếp theo (hoặc dừng khi hết). Hàng đợi tắt thì để YouTube xử lý như cũ.
  window.addEventListener('ended', function(e){
    if (!window.__ytTabletQueueOn) return;
    // Chặn sự kiện này ở window nên listener saveProgress ở document không chạy; ghi tiến độ tại đây.
    saveProgress();
    e.stopImmediatePropagation();
    if (window.__ytTabletEndSent) return;
    window.__ytTabletEndSent = true;
    AndroidPlayback.onState(false);
    AndroidPlayback.onQueueEnded();
  }, true);
  // Video mới bắt đầu phát thì cho phép báo kết thúc lần nữa.
  window.addEventListener('play', function(){ window.__ytTabletEndSent = false; }, true);
  // Ticket #17: trình phát hiện lỗi (ví dụ .ytp-error). Gửi nguyên văn thông báo; app chỉ xử lý lỗi nội dung.
  // Bỏ qua khi máy mất mạng để không nhầm lỗi kết nối với video không khả dụng. Selector này là giả định, chưa kiểm chứng.
  var reportedErrorId = null;
  setInterval(function(){
    if (location.pathname !== '/watch' || navigator.onLine === false) return;
    var id = currentVideoId();
    var err = document.querySelector('.ytp-error');
    if (!id || !err || err.getClientRects().length === 0) return;
    if (reportedErrorId === id) return;
    reportedErrorId = id;
    AndroidPlayback.onUnavailable(id, (err.textContent || '').trim());
  }, 2000);
})();
