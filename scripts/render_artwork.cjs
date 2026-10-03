// Install sharp in a tooling environment; generated PNGs are bundled with the app.
const sharp = require(process.env.SHARP_MODULE || 'sharp');
const path = require('path');
const root=path.resolve(__dirname,'../app/src/main');
(async()=>{
  for(const [source,name,width] of [['crescent_variation_3.svg','journey_crescent',512],['mosaic_variation_2.svg','mosaic_currency',160],['app_icon.svg','app_icon_foreground',288]]) {
    await sharp(path.join(root,'assets',source)).resize({width}).png().toFile(path.join(root,'res/drawable-nodpi',name+'.png'));
  }
})();
