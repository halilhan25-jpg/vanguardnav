const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
(async()=>{
 const browser=await chromium.launch({channel:"chrome",headless:true,args:["--no-sandbox"]});
 try {
 const ctx=await browser.newContext({viewport:{width:393,height:852},permissions:["geolocation"],geolocation:{latitude:36.8969,longitude:30.7133,accuracy:8}});
 const page=await ctx.newPage(),errors=[];
 page.on("pageerror",e=>errors.push(e.message));
 const origin="http://localhost:8080/";
 await page.goto(origin,{waitUntil:"domcontentloaded"});
 const click=id=>page.locator("#"+id).click();
 await page.waitForFunction(()=>typeof ready!=="undefined"&&ready);
 await page.waitForFunction(()=>navigator.serviceWorker.controller!==null);
 await click("quickAddPinBtn");await click("atCenter");
 await page.locator("#pinName").fill("Geyikbayırı kamp");
 await page.locator("#pinText").fill("Sırtın güney yamacı.");
 await page.locator("#newPhotos").setInputFiles({name:"saha.png",mimeType:"image/png",buffer:Buffer.from("iVBORw0KGgoAAAANSUhEUgAAABAAAAAMCAIAAADkharWAAAAFUlEQVR4nGP8z0AaYCJR/aiGEaMBAGavARexRZOyAAAAAElFTkSuQmCC","base64")});
 await page.locator("#pinPhotos input").fill("Manzara");
 await click("savePin");await page.locator("#pinCard img").waitFor();await click("closeJournal");
 await click("navDraw");
 await page.locator("#map").click({position:{x:100,y:240}});
 await page.waitForFunction(()=>document.querySelector("#drawCount").textContent.includes("1 nokta"));
 await page.locator("#map").click({position:{x:175,y:290}});
 await page.waitForFunction(()=>document.querySelector("#drawCount").textContent.includes("2 nokta"));
 await click("nameRoad");await page.locator("#roadName").fill("İlk patika");await click("finishRoad");
 await click("navViews");await click("offlineManager");
 for(const kind of ["terrain","sat"]){
  await page.locator("#regionName").fill("Termessos "+kind);await page.locator("#regionType").selectOption(kind);
  await click("getRegion");await page.waitForFunction(()=>!document.querySelector("#getRegion").disabled,null,{timeout:90000});
  assert.match(await page.locator("#dataMessage").textContent(),/Bölge telefonda hazır/);console.log(kind+" online map saved");
 }
 await click("dataClose");await click("navViews");await click("drawerBackupBtn");
 const wait=page.waitForEvent("download");await click("fullOut");const backup=JSON.parse(await fs.readFile(await (await wait).path(),"utf8"));
 assert.equal(backup.regions.length,2);assert.equal(backup.roads.length,1);assert.equal(backup.pins[0].photos[0].caption,"Manzara");
 await ctx.setOffline(true);await page.reload({waitUntil:"domcontentloaded"});await page.waitForFunction(()=>ready&&pins.length===1&&regions.length===2);
 await click("quickAddPinBtn");await page.locator("#pinRows button").first().click();await page.locator("#pinCard img").waitFor();
 console.log("Road, photo/per-photo note, JSON backup, cold offline reload and pin read passed.");
 const other=await browser.newContext();const fresh=await other.newPage();
 await fresh.goto(origin,{waitUntil:"domcontentloaded"});await fresh.waitForFunction(()=>ready);
 await fresh.locator("#navViews").click();await fresh.locator("#drawerBackupBtn").click();
 await fresh.locator("#backupFile").setInputFiles({name:"backup.json",mimeType:"application/json",buffer:Buffer.from(JSON.stringify(backup))});
 await fresh.waitForFunction(()=>document.querySelector("#dataMessage").textContent.includes("Yedek eklendi"));
 assert.equal(await fresh.evaluate(()=>pins[0].photos[0].caption),"Manzara");assert.equal(await fresh.evaluate(()=>roads[0].name),"İlk patika");
 assert.deepEqual(errors,[]);console.log("Restore into a separate browser profile passed.");
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
