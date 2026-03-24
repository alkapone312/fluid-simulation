const ivec3 offsets3D[27] = ivec3[27](
    ivec3(-1, -1, -1), ivec3(-1, -1, 0), ivec3(-1, -1, 1),
    ivec3(-1,  0, -1), ivec3(-1,  0, 0), ivec3(-1,  0, 1),
    ivec3(-1,  1, -1), ivec3(-1,  1, 0), ivec3(-1,  1, 1),
    ivec3( 0, -1, -1), ivec3( 0, -1, 0), ivec3( 0, -1, 1),
    ivec3( 0,  0, -1), ivec3( 0,  0, 0), ivec3( 0,  0, 1),
    ivec3( 0,  1, -1), ivec3( 0,  1, 0), ivec3( 0,  1, 1),
    ivec3( 1, -1, -1), ivec3( 1, -1, 0), ivec3( 1, -1, 1),
    ivec3( 1,  0, -1), ivec3( 1,  0, 0), ivec3( 1,  0, 1),
    ivec3( 1,  1, -1), ivec3( 1,  1, 0), ivec3( 1,  1, 1)
);

const uint hashK1 = 15823u;
const uint hashK2 = 9737333u;
const uint hashK3 = 440817757u;

ivec3 GetCell3D(vec3 position, float radius)
{
    return ivec3(floor(position / radius));
}

uint HashCell3D(ivec3 cell)
{
    uvec3 uCell = uvec3(cell);
    return (uCell.x * hashK1) + (uCell.y * hashK2) + (uCell.z * hashK3);
}

uint KeyFromHash(uint hash, uint tableSize)
{
    return hash % tableSize;
}

#define DEFINE_SPATIAL_HASH void calculateOffsets(uint i) { \
 \
    if (i >= SPATIAL_HASH_SIZE) return; \
 \
    uint key = SPATIAL_HASH_INDICES[i].z; \
    uint keyPrev = (i == 0) ? SPATIAL_HASH_SIZE : SPATIAL_HASH_INDICES[i - 1].z; \
 \
    if (key != keyPrev) { \
        SPATIAL_HASH_OFFSETS[key] = i; \
    } \
} \
\
void UpdateSpatialHash(uint i) { \
    if (i >= SPATIAL_HASH_SIZE) return; \
    SPATIAL_HASH_OFFSETS[i] = uint(SPATIAL_HASH_SIZE); \
 \
    uint index = i; \
 \
    ivec3 cell = GetCell3D(SPATIAL_HASH_POSITIONS[index].xyz, SPATIAL_HASH_CELL_SIZE); \
    uint hash = HashCell3D(cell); \
    uint key = KeyFromHash(hash, uint(numParticles)); \
 \
    SPATIAL_HASH_INDICES[i] = uvec3(index, hash, key); \
} \


#define SPATIAL_HASH_NEIGHBOUR_LOOP(POSITION, RADIUS, DEFINE, CODE) \
ivec3 originCell = GetCell3D(POSITION, RADIUS); \
for (int i = 0; i < 27; i ++) \
{ \
    uint hash = HashCell3D(originCell + offsets3D[i]); \
    uint key = KeyFromHash(hash, SPATIAL_HASH_SIZE); \
    uint currIndex = SPATIAL_HASH_OFFSETS[key]; \
 \
    while (currIndex < SPATIAL_HASH_SIZE) \
    { \
        uvec3 DEFINE = uvec3(SPATIAL_HASH_INDICES[currIndex]); \
        currIndex++; \
        if (DEFINE.z != key) break; \
        if (DEFINE.y != hash) continue; \
 \
        CODE \
    } \
} \
