import {mkdir,cp} from 'node:fs/promises';
export async function prepareClientVendor(){await mkdir(new URL('../web/vendor/',import.meta.url),{recursive:true});for(const [source,destination] of [['dist/hls.mjs','hls.mjs'],['LICENSE','hls-LICENSE.txt']])await cp(new URL('../node_modules/hls.js/'+source,import.meta.url),new URL('../web/vendor/'+destination,import.meta.url));}
