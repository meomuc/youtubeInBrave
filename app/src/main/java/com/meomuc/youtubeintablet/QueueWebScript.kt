package com.meomuc.youtubeintablet

/**
 * JavaScript gắn nút hàng đợi lên trang YouTube (ticket #19):
 * - Trang xem video (/watch): hai nút "Phát tiếp theo" và "Thêm vào hàng đợi" ngay bên trái nút thích.
 * - Cover (trang chủ, tìm kiếm, kênh): một nút tròn góc trên bên phải, bấm = thêm vào cuối hàng đợi.
 * Cả hai chỉ hiện khi hàng đợi bật (window.__ytTabletQueueOn); trang chỉ hiện nút ở /watch.
 * Dùng MutationObserver vì YouTube dựng lại DOM khi chuyển trang (SPA) và khi cuộn.
 *
 * Hợp đồng với app: AndroidQueue.add(id, title, next) (xem QueueBridge trong MainActivity).
 */
object QueueWebScript {

    val SCRIPT: String = """
(function(){
  if (window.__ytTabletQueueUi) return;
  window.__ytTabletQueueUi = true;
  var WATCH = '${WatchLinkRules.WATCH_PATH}';
  var ID_RE = new RegExp('${WatchLinkRules.HREF_PATTERN_SOURCE}');
  var PATH_ADD = "${PhosphorIcons.path("list-plus")}";
  var PATH_NEXT = "${PhosphorIcons.path("skip-forward")}";
  var LABEL_ADD = 'Thêm vào hàng đợi';
  var LABEL_NEXT = 'Phát tiếp theo';
  var WATCH_ATTR = 'data-yt-tablet-watch';
  var COVER_ATTR = 'data-yt-tablet-cover';
  var COVER_BTN_ATTR = 'data-yt-tablet-cover-btn';
  var COVER_SELECTOR = 'ytm-rich-item-renderer, ytm-media-item';

  function queueOn(){ return !!window.__ytTabletQueueOn; }
  function isWatch(){ return location.pathname === WATCH; }

  function add(id, title, next){
    if (!id || !window.AndroidQueue) return;
    try { AndroidQueue.add(id, title || '', next); } catch (e) {}
  }

  function currentWatchId(){
    try { return new URL(location.href).searchParams.get('v') || ''; } catch (e) { return ''; }
  }
  function currentWatchTitle(){
    return (document.title || '').replace(/\s*-\s*YouTube\s*$/, '').trim();
  }

  // Dựng SVG bằng DOM (không dùng innerHTML) vì YouTube bật Trusted Types.
  function svgIcon(d){
    var NS = 'http://www.w3.org/2000/svg';
    var svg = document.createElementNS(NS, 'svg');
    svg.setAttribute('viewBox', '0 0 256 256');
    svg.setAttribute('width', '24');
    svg.setAttribute('height', '24');
    svg.setAttribute('fill', 'currentColor');
    svg.setAttribute('aria-hidden', 'true');
    var p = document.createElementNS(NS, 'path');
    p.setAttribute('d', d);
    svg.appendChild(p);
    return svg;
  }

  function roundButton(label, d, size, onClick){
    var b = document.createElement('button');
    b.type = 'button';
    b.setAttribute('aria-label', label);
    b.title = label;
    b.appendChild(svgIcon(d));
    b.style.cssText = 'width:' + size + 'px;height:' + size + 'px;min-width:' + size + 'px;border:0;border-radius:50%;' +
      'background:#F2F2F2;color:#0F0F0F;display:inline-flex;align-items:center;justify-content:center;padding:0;margin:0;';
    b.addEventListener('click', function(e){ e.preventDefault(); e.stopPropagation(); onClick(); });
    return b;
  }

  function ensureWatchBar(){
    var actions = document.querySelector('div.slim-video-action-bar-actions');
    if (!actions || actions.querySelector('[' + WATCH_ATTR + ']')) return;
    var like = actions.querySelector(':scope > like-button-view-model');
    if (!like) return;
    var wrap = document.createElement('div');
    wrap.setAttribute(WATCH_ATTR, '1');
    wrap.style.cssText = 'display:none;align-items:center;gap:8px;margin-right:8px;';
    wrap.appendChild(roundButton(LABEL_NEXT, PATH_NEXT, 48, function(){ add(currentWatchId(), currentWatchTitle(), true); }));
    wrap.appendChild(roundButton(LABEL_ADD, PATH_ADD, 48, function(){ add(currentWatchId(), currentWatchTitle(), false); }));
    actions.insertBefore(wrap, like);
  }

  function ensureCover(item){
    if (item.hasAttribute(COVER_ATTR)) return;
    var link = item.querySelector('a[href^="/watch?v="]');
    if (!link) return;
    var m = ID_RE.exec(link.getAttribute('href') || '');
    if (!m) return;
    var id = m[1];
    item.setAttribute(COVER_ATTR, '1');
    if (getComputedStyle(item).position === 'static') item.style.position = 'relative';
    var btn = roundButton(LABEL_ADD, PATH_ADD, 36, function(){
      var heading = item.querySelector('h3, [role="heading"]');
      var title = (link.getAttribute('title') || link.getAttribute('aria-label') ||
        (heading && heading.textContent) || link.textContent || '').trim();
      add(id, title, false);
    });
    btn.setAttribute(COVER_BTN_ATTR, '1');
    btn.style.position = 'absolute';
    btn.style.top = '8px';
    btn.style.right = '8px';
    btn.style.zIndex = '10';
    btn.style.display = queueOn() ? 'inline-flex' : 'none';
    item.appendChild(btn);
  }

  function update(){
    var on = queueOn();
    var wrap = document.querySelector('[' + WATCH_ATTR + ']');
    if (wrap) wrap.style.display = (on && isWatch()) ? 'flex' : 'none';
    var covers = document.querySelectorAll('[' + COVER_BTN_ATTR + ']');
    for (var i = 0; i < covers.length; i++) covers[i].style.display = on ? 'inline-flex' : 'none';
  }

  function sync(){
    ensureWatchBar();
    document.querySelectorAll(COVER_SELECTOR).forEach(ensureCover);
    update();
  }

  var pending = false;
  function schedule(){
    if (pending) return;
    pending = true;
    requestAnimationFrame(function(){ pending = false; sync(); });
  }

  window.__ytTabletUpdateQueueBar = function(){ update(); schedule(); };
  document.addEventListener('yt-navigate-finish', window.__ytTabletUpdateQueueBar, true);
  new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true });
  sync();
})();
"""
}
