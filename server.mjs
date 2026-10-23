import http from 'node:http';
import {readFile} from 'node:fs/promises';
import {resolve,extname,sep} from 'node:path';
import handler from './api/index.mjs';
const root=resolve('web');
http.createServer(async(req,res)=>{
 if(req.url.startsWith('/api')) return handler(req,res);
 try{
 const path=resolve(root,'.'+decodeURIComponent(new URL(req.url,'http://localhost').pathname));
 if(path!==root&&!path.startsWith(root+sep)){res.writeHead(403);return res.end();}
 const file=path===root?resolve(root,'index.html'):path;
 res.setHeader('Content-Type',({'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml'})[extname(file)]||'application/octet-stream');
 res.setHeader('X-Content-Type-Options','nosniff');res.end(await readFile(file));
 }catch{res.writeHead(404);res.end('Not found');}
}).listen(Number(process.env.PORT||3000),'127.0.0.1');
