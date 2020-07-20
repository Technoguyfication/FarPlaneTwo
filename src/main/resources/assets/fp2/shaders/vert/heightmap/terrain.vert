/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-$today.year DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

void main(){
    ivec2 posXZ = position_offset + in_offset_absolute;

    HEIGHTMAP_TYPE center = sampleHeightmap(posXZ);

    dvec3 pos = dvec3(double(posXZ.x), double(unpackHeight(center)) + .5, double(posXZ.y));

    //give raw position to fragment shader
    vs_out.pos = vec3(pos);

    //translate vertex position
    gl_Position = transformPoint(vec3(pos - camera.position));

    //decode sky and block light
    vs_out.light = vec2(ivec2(unpackLight(center)) >> ivec2(0, 4) & 0xF) / 16.;

    //store block state
    vs_out.state = unpackBlock(center);

    int biome = unpackBiome(center);
    if (isGrass(unpackFlags(center))) { //grass
        if (IS_SWAMP) {
            vs_out.color = fromRGB(-1. < -.1 ? 0x4C763C : 0x6A7039);
        } else if (IS_ROOFED_FOREST)    {
            vec4 original = getGrassColorAtPos(pos, biome);
            vs_out.color = vec4(((original + fromARGB(0x0028340A)) * .5).rgb, original.a);
        } else if (IS_MESA) {
            vs_out.color = fromRGB(0x90814D);
        } else {
            vs_out.color = getGrassColorAtPos(pos, biome);
        }
    } else if (isFoliage(unpackFlags(center)))  { //foliage
        if (IS_SWAMP) {
            vs_out.color = fromRGB(0x6A7039);
        } else if (IS_MESA) {
            vs_out.color = fromRGB(0x9E814D);
        } else {
            vs_out.color = getFoliageColorAtPos(pos, biome);
        }
    } else {
        vs_out.color = vec4(1.);
    }
}
