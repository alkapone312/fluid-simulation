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