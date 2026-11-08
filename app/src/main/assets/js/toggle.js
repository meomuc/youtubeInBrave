(function(){var v=document.querySelector('video');if(!v){return;}
if(v.paused){window.__ytTabletUserPaused=false;v.play();}
else{window.__ytTabletUserPaused=true;v.pause();}})();
